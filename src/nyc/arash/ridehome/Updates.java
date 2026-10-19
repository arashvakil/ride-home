package nyc.arash.ridehome;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.Toast;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Release-only, private-cache updates. Network and APK work never run on the UI thread. */
final class Updates {
  private static Updates instance;

  static synchronized Updates get(Context context) {
    if (instance == null) instance = new Updates(context.getApplicationContext());
    return instance;
  }

  private final Context context;
  private final SharedPreferences prefs;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private volatile JSONObject ready;
  private volatile boolean busy;
  private volatile String status = "Checks run when you return Home, at most once every six hours.";
  private boolean loaded;

  private Updates(Context c) {
    context = c;
    prefs = c.getSharedPreferences("updates", 0);
  }

  boolean automatic() {
    return prefs.getBoolean("automatic", true);
  }

  void automatic(boolean enabled) {
    prefs.edit().putBoolean("automatic", enabled).apply();
  }

  String version() {
    try {
      return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (Exception e) {
      return "Unknown";
    }
  }

  String readyVersion() {
    JSONObject r = ready;
    return r == null ? null : r.optString("versionName");
  }

  void check(Home home, boolean manual) {
    if (!BuildConfig.RELEASE) {
      if (manual) {
        status = "Developer builds do not use the public update channel.";
        show(home);
      }
      return;
    }
    if (busy) {
      if (manual) Toast.makeText(home, "Checking for updates…", Toast.LENGTH_SHORT).show();
      return;
    }
    long now = System.currentTimeMillis();
    boolean fetch =
        manual || (automatic() && UpdatePolicy.due(now, prefs.getLong("last_check", 0)));
    if (loaded && !fetch) return;
    busy = true;
    if (fetch) {
      prefs.edit().putLong("last_check", now).apply();
      status = "Checking for updates…";
      if (manual) Toast.makeText(home, status, Toast.LENGTH_SHORT).show();
    }
    worker.execute(
        () -> {
          try {
            loadReady();
            if (fetch) {
              JSONObject manifest =
                  new JSONObject(new String(read(UpdatePolicy.CHANNEL, 65536), "UTF-8"));
              validateManifest(manifest);
              if (manifest.getInt("versionCode") <= installed().versionCode) {
                clearReady();
                status = "You have the latest release.";
              } else {
                File apk = apk();
                if (!verified(apk, manifest)) {
                  clearReady();
                  File partial = new File(directory(), "download.part");
                  try {
                    download(manifest.getString("apkUrl"), partial, 64 * 1024 * 1024);
                    if (!verified(partial, manifest))
                      throw new IOException("Update verification failed");
                    if (!partial.renameTo(apk)) throw new IOException("Cannot save update");
                  } finally {
                    partial.delete();
                  }
                }
                prefs.edit().putString("ready", manifest.toString()).apply();
                ready = manifest;
                status =
                    "Update "
                        + manifest.getString("versionName")
                        + " is ready. Favorites and Home tab settings are kept.";
              }
            }
          } catch (Exception e) {
            status = "Could not check for updates. Check your connection and try again.";
            if (ready != null) status += " Your previously verified update is still available.";
          } finally {
            loaded = true;
            busy = false;
            main.post(
                () -> {
                  if (!home.isFinishing() && !home.isDestroyed() && Home.visible) {
                    home.render();
                    if (manual) show(home);
                  }
                });
          }
        });
  }

  void show(Home home) {
    List<String> labels = new ArrayList<>();
    List<Runnable> actions = new ArrayList<>();
    if (ready != null) {
      labels.add("Install " + readyVersion());
      actions.add(() -> install(home));
    }
    labels.add(busy ? "Checking…" : "Check now");
    actions.add(() -> check(home, true));
    labels.add("Automatic checks: " + (automatic() ? "On" : "Off"));
    actions.add(
        () -> {
          automatic(!automatic());
          show(home);
        });
    home.sheet("Ride Home " + version(), status, labels, actions);
  }

  private void install(Home home) {
    if (busy || ready == null) return;
    if (Build.VERSION.SDK_INT >= 26 && !context.getPackageManager().canRequestPackageInstalls()) {
      new AlertDialog.Builder(home)
          .setTitle("Allow Ride Home updates")
          .setMessage(
              "Allow installs from Ride Home in Android settings, then return and tap Install"
                  + " again.")
          .setPositiveButton(
              "Open settings",
              (d, w) ->
                  home.open(
                      new Intent(
                          Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                          Uri.parse("package:" + context.getPackageName()))))
          .setNegativeButton("Later", null)
          .show();
      return;
    }
    final JSONObject candidate = ready;
    busy = true;
    Toast.makeText(home, "Verifying update…", Toast.LENGTH_SHORT).show();
    worker.execute(
        () -> {
          try {
            if (!verified(apk(), candidate)) {
              clearReady();
              throw new IOException("Invalid cached update");
            }
            PackageInstaller installer = context.getPackageManager().getPackageInstaller();
            int old = prefs.getInt("session", -1);
            if (old >= 0)
              try {
                installer.abandonSession(old);
              } catch (Exception ignored) {
              }
            PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(context.getPackageName());
            int id = installer.createSession(params);
            prefs.edit().putInt("session", id).apply();
            try (PackageInstaller.Session session = installer.openSession(id)) {
              try (InputStream in = new FileInputStream(apk());
                  OutputStream out = session.openWrite("base.apk", 0, apk().length())) {
                byte[] buffer = new byte[32768];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                session.fsync(out);
              }
              Intent callback =
                  new Intent(context, UpdateInstallReceiver.class)
                      .setAction(context.getPackageName() + ".UPDATE_INSTALL");
              int flags = PendingIntent.FLAG_UPDATE_CURRENT;
              if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
              session.commit(
                  PendingIntent.getBroadcast(context, id, callback, flags).getIntentSender());
            } catch (Exception failure) {
              installer.abandonSession(id);
              prefs.edit().remove("session").apply();
              throw failure;
            }
          } catch (Exception e) {
            main.post(
                () ->
                    Toast.makeText(
                            context,
                            "Cannot install update. Check again or download from GitHub Releases.",
                            Toast.LENGTH_LONG)
                        .show());
          } finally {
            busy = false;
          }
        });
  }

  private void validateManifest(JSONObject m) throws Exception {
    if (!UpdatePolicy.manifest(
        m.getInt("versionCode"),
        m.getString("versionName"),
        m.getString("apkUrl"),
        m.getString("sha256"),
        m.getString("packageName"))) throw new IOException("Invalid update manifest");
  }

  private void loadReady() throws Exception {
    String value = prefs.getString("ready", null);
    if (value == null) return;
    try {
      JSONObject m = new JSONObject(value);
      if (!verified(apk(), m)) {
        clearReady();
        return;
      }
      ready = m;
      status = "Update " + m.getString("versionName") + " is ready to install.";
    } catch (Exception e) {
      clearReady();
    }
  }

  private void clearReady() {
    ready = null;
    prefs.edit().remove("ready").apply();
    apk().delete();
  }

  private File directory() {
    File dir = new File(context.getFilesDir(), "updates");
    if (!dir.isDirectory()) dir.mkdirs();
    return dir;
  }

  private File apk() {
    return new File(directory(), "ready.apk");
  }

  @SuppressWarnings("deprecation")
  private PackageInfo installed() throws Exception {
    return context.getPackageManager().getPackageInfo(context.getPackageName(), flags());
  }

  @SuppressWarnings("deprecation")
  private int flags() {
    // Early Android 9 firmware collects archive certificates only with the legacy flag.
    // Request both, but still compare the current signer set from SigningInfo on API 28+.
    return Build.VERSION.SDK_INT >= 28
        ? (PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES)
        : PackageManager.GET_SIGNATURES;
  }

  @SuppressWarnings("deprecation")
  private Set<String> signers(PackageInfo info) throws Exception {
    Signature[] signatures =
        Build.VERSION.SDK_INT >= 28
            ? (info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners())
            : info.signatures;
    Set<String> values = new HashSet<>();
    if (signatures != null)
      for (Signature signature : signatures)
        values.add(hex(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())));
    return values;
  }

  private boolean verified(File file, JSONObject m) throws Exception {
    validateManifest(m);
    if (!file.isFile() || file.length() > 64 * 1024 * 1024) return false;
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (InputStream in = new FileInputStream(file)) {
      byte[] buffer = new byte[32768];
      int count;
      while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
    }
    if (!hex(digest.digest()).equalsIgnoreCase(m.getString("sha256"))) return false;
    PackageInfo current = installed();
    PackageInfo archive =
        context.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), flags());
    return archive != null
        && UpdatePolicy.apk(
            current.packageName,
            current.versionCode,
            signers(current),
            archive.packageName,
            archive.versionCode,
            signers(archive),
            m.getInt("versionCode"));
  }

  private static String hex(byte[] bytes) {
    StringBuilder out = new StringBuilder();
    for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 255));
    return out.toString();
  }

  private byte[] read(String url, int limit) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    transfer(url, out, limit);
    return out.toByteArray();
  }

  private void download(String url, File file, int limit) throws Exception {
    try (OutputStream out = new FileOutputStream(file)) {
      transfer(url, out, limit);
    }
  }

  private void transfer(String url, OutputStream out, int limit) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(3);
    for (int redirects = 0; redirects <= 5; redirects++) {
      if (!UpdatePolicy.secureTransport(url)) throw new IOException("Untrusted transport");
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(15000);
      connection.setReadTimeout(30000);
      connection.setRequestProperty("User-Agent", "RideHome-Updater");
      try {
        int status = connection.getResponseCode();
        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
          String location = connection.getHeaderField("Location");
          if (location == null) throw new IOException("Missing redirect");
          url = new URL(new URL(url), location).toString();
          continue;
        }
        if (status != 200 || connection.getContentLength() > limit)
          throw new IOException("Download unavailable");
        try (InputStream in = connection.getInputStream()) {
          int total = 0, count;
          byte[] buffer = new byte[16384];
          while ((count = in.read(buffer)) != -1) {
            total += count;
            if (total > limit || System.nanoTime() > deadline)
              throw new IOException("Download limit exceeded");
            out.write(buffer, 0, count);
          }
        }
        return;
      } finally {
        connection.disconnect();
      }
    }
    throw new IOException("Too many redirects");
  }
}
