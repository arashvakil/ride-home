package nyc.arash.ridehome;

import android.content.*;
import android.content.pm.*;
import java.util.*;

final class AppCatalog {
  static class App {
    ResolveInfo info;
    String pkg, name;
    int color;

    App(Context c, ResolveInfo r) {
      info = r;
      pkg = r.activityInfo.packageName;
      name = r.loadLabel(c.getPackageManager()).toString();
      color = 0xFF3E527D;
      if (pkg.equals("com.plexapp.android")) {
        color = 0xFF8A6222;
      }
      if (pkg.startsWith("com.google.android.youtube")) {
        color = 0xFF8E2443;
      }
      if (pkg.equals("com.android.chrome") || pkg.equals("com.microsoft.emmx")) {
        color = 0xFF315FA1;
      }
      if (pkg.equals("com.strava")) {
        color = 0xFF9C4926;
      }
    }
  }

  static List<App> all(Context c) {
    List<ResolveInfo> raw =
        c.getPackageManager()
            .queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
    List<App> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (ResolveInfo r : raw) {
      String p = r.activityInfo.packageName;
      if (p.equals(c.getPackageName()) || !seen.add(p)) continue;
      out.add(new App(c, r));
    }
    Collections.sort(out, (a, b) -> a.name.compareToIgnoreCase(b.name));
    return out;
  }

  static List<String> order(Context c) {
    SharedPreferences preferences = c.getSharedPreferences("home", 0);
    return FavoriteOrder.read(
        preferences.getString("favorite_order", null), preferences.getStringSet("favorites", null));
  }

  static void save(Context c, List<String> order) {
    c.getSharedPreferences("home", 0)
        .edit()
        .putString("favorite_order", android.text.TextUtils.join("|", order))
        .apply();
  }

  static List<App> favorites(Context c, List<App> all) {
    List<App> out = new ArrayList<>();
    for (String pkg : order(c))
      for (App app : all)
        if (pkg.equals(app.pkg)) {
          out.add(app);
          break;
        }
    return out;
  }
}
