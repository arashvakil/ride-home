package nyc.arash.ridehome;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.graphics.*;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.*;

public class Navigation extends AccessibilityService {
  static Navigation live;
  WindowManager wm;
  View overlay;
  WindowManager.LayoutParams layout;
  boolean tray = false, homeVisible = false;
  Handler handler = new Handler(Looper.getMainLooper());
  Runnable timeout = () -> dismissTray();

  int dp(float n) {
    return Ui.dp(this, n);
  }

  SharedPreferences prefs() {
    return getSharedPreferences("home", 0);
  }

  static void home(boolean visible) {
    if (live != null) {
      live.homeVisible = visible;
      if (visible) {
        live.removeOverlay();
      } else if (!live.tray) live.showTab();
    }
  }

  static void refresh() {
    if (live != null) {
      live.removeOverlay();
      if (!live.homeVisible) live.showTab();
    }
  }

  @Override
  protected void onServiceConnected() {
    live = this;
    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    homeVisible = Home.visible;
    if (!homeVisible) showTab();
    sendBroadcast(new Intent("nyc.arash.ridehome.NAVIGATION_STATE").setPackage(getPackageName()));
  }

  Point screen() {
    Point p = new Point();
    wm.getDefaultDisplay().getRealSize(p);
    return p;
  }

  void removeOverlay() {
    handler.removeCallbacks(timeout);
    if (overlay != null) {
      wm.removeView(overlay);
      overlay = null;
    }
    tray = false;
  }

  WindowManager.LayoutParams params(int w, int h) {
    WindowManager.LayoutParams p =
        new WindowManager.LayoutParams(
            w,
            h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
    p.gravity = Gravity.TOP | Gravity.LEFT;
    return p;
  }

  void goHome() {
    removeOverlay();
    startActivity(
        new Intent(this, Home.class)
            .putExtra("favorites", true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
  }

  void showTab() {
    removeOverlay();
    if (homeVisible) return;
    Point bounds = screen();
    LinearLayout tab = new LinearLayout(this);
    tab.setGravity(Gravity.CENTER);
    tab.setPadding(dp(12), 0, dp(12), 0);
    tab.setBackground(Ui.shape(0xF0222D43, 24, this));
    tab.setElevation(dp(8));
    TextView label = Ui.text(this, "⌂  Home", 16, Ui.WHITE);
    label.setTypeface(Typeface.create("sans-serif-medium", 0));
    label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    tab.addView(label);
    tab.setContentDescription("Home. Hold for controls. Drag to move.");
    tab.setFocusable(true);
    tab.setClickable(true);
    tab.setLongClickable(true);
    tab.setOnClickListener(v -> goHome());
    tab.setOnLongClickListener(
        v -> {
          showTray();
          return true;
        });
    layout = params(dp(108), dp(52));
    layout.x = prefs().getBoolean("tab_right", true) ? bounds.x - layout.width : 0;
    layout.y =
        NavGeometry.restoreY(
            prefs().getFloat("tab_y", .48f), bounds.y, layout.height, dp(28), dp(16));
    layout.setTitle("Ride Home tab");
    tab.setOnTouchListener(
        new View.OnTouchListener() {
          float downX, downY;
          int startX, startY;
          boolean moved, held;
          Runnable hold =
              () -> {
                if (overlay != tab) return;
                held = true;
                showTray();
              };

          public boolean onTouch(View v, MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
              downX = e.getRawX();
              downY = e.getRawY();
              startX = layout.x;
              startY = layout.y;
              moved = false;
              held = false;
              handler.postDelayed(hold, ViewConfiguration.getLongPressTimeout());
              return true;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
              if (held || overlay != v) return true;
              float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
              if (Math.hypot(dx, dy)
                  > ViewConfiguration.get(Navigation.this).getScaledTouchSlop()) {
                moved = true;
                handler.removeCallbacks(hold);
              }
              if (moved) {
                layout.x = clamp(startX + Math.round(dx), 0, bounds.x - layout.width);
                layout.y =
                    clamp(startY + Math.round(dy), dp(28), bounds.y - layout.height - dp(16));
                wm.updateViewLayout(overlay, layout);
              }
              return true;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_UP
                || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
              handler.removeCallbacks(hold);
              if (held || overlay != v) return true;
              if (moved) {
                layout.x = NavGeometry.edgeX(bounds.x, layout.width, layout.x);
                wm.updateViewLayout(overlay, layout);
                prefs()
                    .edit()
                    .putBoolean("tab_right", layout.x > 0)
                    .putFloat("tab_y", (float) layout.y / bounds.y)
                    .apply();
              } else if (e.getActionMasked() == MotionEvent.ACTION_UP) v.performClick();
              return true;
            }
            return true;
          }
        });
    overlay = tab;
    wm.addView(tab, layout);
    tab.setAlpha(0);
    tab.animate().alpha(1).setDuration(160).start();
  }

  int clamp(int n, int low, int high) {
    return NavGeometry.clamp(n, low, high);
  }

  void showTray() {
    removeOverlay();
    tray = true;
    FrameLayout scrim = new FrameLayout(this);
    scrim.setBackgroundColor(0x77060A13);
    scrim.setContentDescription("Controls. Tap outside to close.");
    scrim.setOnClickListener(v -> dismissTray());
    LinearLayout panel = Ui.column(this);
    panel.setPadding(dp(30), dp(24), dp(30), dp(26));
    panel.setBackground(Ui.shape(0xFF1A2335, 30, this));
    panel.setElevation(dp(18));
    panel.setOnClickListener(v -> scheduleDismiss());
    FrameLayout.LayoutParams pp =
        new FrameLayout.LayoutParams(
            Math.min(dp(720), screen().x - dp(48)), -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    pp.bottomMargin = dp(40);
    scrim.addView(panel, pp);
    LinearLayout titleRow = new LinearLayout(this);
    titleRow.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = Ui.text(this, "Controls", 27, Ui.WHITE);
    title.setTypeface(Typeface.create("sans-serif-medium", 0));
    titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    TextView close = Ui.button(this, "Close", () -> dismissTray());
    titleRow.addView(close, new LinearLayout.LayoutParams(-2, dp(50)));
    panel.addView(titleRow);
    LinearLayout actions = new LinearLayout(this);
    actions.setPadding(0, dp(24), 0, dp(26));
    panel.addView(actions);
    control(actions, "Back", () -> global(GLOBAL_ACTION_BACK));
    control(actions, "Home", () -> goHome());
    control(actions, "Apps", () -> global(GLOBAL_ACTION_RECENTS));
    control(
        actions,
        "Settings",
        () -> {
          dismissTray();
          startActivity(
              new Intent(android.provider.Settings.ACTION_SETTINGS)
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        });
    AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    TextView volume =
        Ui.text(
            this,
            "Volume  " + audio.getStreamVolume(AudioManager.STREAM_MUSIC) + " / " + max,
            16,
            Ui.MUTED);
    panel.addView(volume);
    SeekBar slider = new SeekBar(this);
    slider.setContentDescription("Media volume");
    slider.setMax(max);
    slider.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
    slider.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
    slider.setThumbTintList(android.content.res.ColorStateList.valueOf(Ui.WHITE));
    LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, dp(58));
    vp.topMargin = dp(8);
    panel.addView(slider, vp);
    slider.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          boolean tracking = false;

          public void onProgressChanged(SeekBar b, int n, boolean user) {
            if (user) {
              audio.setStreamVolume(AudioManager.STREAM_MUSIC, n, 0);
              volume.setText("Volume  " + n + " / " + max);
              if (!tracking) scheduleDismiss();
            }
          }

          public void onStartTrackingTouch(SeekBar b) {
            tracking = true;
            handler.removeCallbacks(timeout);
          }

          public void onStopTrackingTouch(SeekBar b) {
            tracking = false;
            scheduleDismiss();
          }
        });
    TextView help = Ui.text(this, "Tap outside to close · Hold Home to open", 14, Ui.MUTED);
    help.setGravity(Gravity.CENTER);
    panel.addView(help);
    scrim.setOnTouchListener(
        (v, e) -> {
          scheduleDismiss();
          return false;
        });
    layout = params(-1, -1);
    layout.setTitle("Ride Home controls");
    overlay = scrim;
    wm.addView(scrim, layout);
    scrim.setAlpha(0);
    scrim.animate().alpha(1).setDuration(180).start();
    panel.setTranslationY(dp(22));
    panel.animate().translationY(0).setDuration(220).start();
    scheduleDismiss();
  }

  void control(LinearLayout row, String name, Runnable action) {
    TextView b = Ui.button(this, name, action);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(64), 1);
    p.setMargins(dp(4), 0, dp(4), 0);
    row.addView(b, p);
  }

  void global(int action) {
    dismissTray();
    handler.postDelayed(
        () -> {
          if (!performGlobalAction(action))
            Toast.makeText(this, "This control is unavailable right now", Toast.LENGTH_SHORT)
                .show();
        },
        80);
  }

  void scheduleDismiss() {
    handler.removeCallbacks(timeout);
    handler.postDelayed(timeout, 8000);
  }

  void dismissTray() {
    removeOverlay();
    if (!homeVisible) showTab();
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    /* No screen content accessed. Home visibility uses our Activity lifecycle. */
  }

  @Override
  public void onInterrupt() {}

  @Override
  public void onConfigurationChanged(android.content.res.Configuration c) {
    super.onConfigurationChanged(c);
    refresh();
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    removeOverlay();
    live = null;
    super.onDestroy();
  }
}
