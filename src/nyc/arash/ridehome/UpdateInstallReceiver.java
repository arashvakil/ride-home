package nyc.arash.ridehome;

import android.content.*;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

/** Explicit PendingIntent callback; not exported to other apps. */
public final class UpdateInstallReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    SharedPreferences prefs = context.getSharedPreferences("updates", 0);
    int session = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1);
    if (session < 0 || session != prefs.getInt("session", -1)) return;
    int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
    if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
      Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
      if (confirm != null)
        try {
          context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
          Toast.makeText(context, "Return Home and tap Install to try again.", Toast.LENGTH_LONG)
              .show();
        }
    } else {
      prefs.edit().remove("session").apply();
      Toast.makeText(
              context,
              status == PackageInstaller.STATUS_SUCCESS
                  ? "Ride Home updated"
                  : "Update was not installed. You can try again in Settings.",
              Toast.LENGTH_LONG)
          .show();
    }
  }
}
