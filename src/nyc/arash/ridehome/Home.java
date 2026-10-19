package nyc.arash.ridehome;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class Home extends Activity {
  static boolean visible;
  boolean all = false, launching = false;
  LinearLayout root;
  Ui.Backdrop backdrop;
  TextView hint;
  List<AppCatalog.App> catalog;
  boolean registered;
  private final BroadcastReceiver serviceState =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          if (visible) render();
        }
      };

  int dp(float n) {
    return Ui.dp(this, n);
  }

  LinearLayout.LayoutParams lp(int w, int h) {
    return new LinearLayout.LayoutParams(w, h);
  }

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    getWindow().setStatusBarColor(0xFF0B101B);
    getWindow().setNavigationBarColor(0xFF0B101B);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    if (intent.getBooleanExtra("favorites", false) || intent.hasCategory(Intent.CATEGORY_HOME))
      all = false;
    if (visible) render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    launching = false;
    visible = true;
    Navigation.home(true);
    registerReceiver(serviceState, new IntentFilter("nyc.arash.ridehome.NAVIGATION_STATE"));
    registered = true;
    render();
    Updates.get(this).check(this, false);
  }

  @Override
  protected void onPause() {
    visible = false;
    Navigation.home(false);
    if (registered) {
      unregisterReceiver(serviceState);
      registered = false;
    }
    super.onPause();
  }

  @Override
  public void onWindowFocusChanged(boolean focus) {
    super.onWindowFocusChanged(focus);
    if (focus)
      getWindow()
          .getDecorView()
          .setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                  | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  }

  void open(Intent i) {
    try {
      startActivity(i);
      overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    } catch (Exception e) {
      launching = false;
      Toast.makeText(this, "This app is unavailable on this tablet", Toast.LENGTH_LONG).show();
    }
  }

  void launch(AppCatalog.App app) {
    if (launching) return;
    launching = true;
    if (hint != null) hint.setText("Opening " + app.name + "…");
    Intent intent = getPackageManager().getLaunchIntentForPackage(app.pkg);
    if (intent == null)
      intent =
          new Intent(Intent.ACTION_MAIN)
              .addCategory(Intent.CATEGORY_LAUNCHER)
              .setComponent(new ComponentName(app.pkg, app.info.activityInfo.name));
    final Intent target = intent;
    root.postDelayed(
        () ->
            open(
                target.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)),
        110);
  }

  void render() {
    catalog = AppCatalog.all(this);
    FrameLayout frame = new FrameLayout(this);
    backdrop = new Ui.Backdrop(this);
    frame.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));
    root = Ui.column(this);
    root.setPadding(dp(76), dp(48), dp(76), dp(36));
    frame.addView(root, new FrameLayout.LayoutParams(-1, -1));
    setContentView(frame);
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    root.addView(header, lp(-1, dp(64)));
    TextView brand = Ui.text(this, "Ride Home", 25, Ui.WHITE);
    brand.setTypeface(Typeface.create("sans-serif-medium", 0));
    header.addView(brand, lp(0, -2));
    ((LinearLayout.LayoutParams) brand.getLayoutParams()).weight = 1;
    chip(
        header,
        all ? "Favorites" : "All Apps",
        () -> {
          all = !all;
          render();
        });
    chip(
        header,
        "Controls",
        () -> {
          if (Navigation.live != null) Navigation.live.showTray();
          else navigationSetup();
        });
    if (Updates.get(this).readyVersion() != null)
      chip(header, "Update available", () -> Updates.get(this).show(this));
    chip(header, "Settings", () -> settings());
    TextClock clock = new TextClock(this);
    clock.setFormat12Hour("h:mm");
    clock.setFormat24Hour("HH:mm");
    clock.setTextSize(22);
    clock.setTextColor(Ui.MUTED);
    clock.setPadding(dp(32), 0, 0, 0);
    header.addView(clock, lp(dp(130), -2));
    if (all) renderAll();
    else renderFavorites();
    LinearLayout footer = new LinearLayout(this);
    footer.setGravity(Gravity.CENTER_VERTICAL);
    root.addView(footer, lp(-1, dp(44)));
    hint =
        Ui.text(
            this,
            Navigation.live == null
                ? "Enable Home access →"
                : "Tap an app · Hold to arrange · Hold Home inside apps for controls",
            15,
            Navigation.live == null ? 0xFFF8D699 : Ui.MUTED);
    footer.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
    if (Navigation.live == null) hint.setOnClickListener(v -> navigationSetup());
    TextClock date = new TextClock(this);
    date.setFormat12Hour("EEEE, MMMM d");
    date.setFormat24Hour("EEEE, MMMM d");
    date.setTextSize(15);
    date.setTextColor(Ui.MUTED);
    footer.addView(date);
  }

  void chip(LinearLayout row, String label, Runnable action) {
    TextView b = Ui.button(this, label, action);
    LinearLayout.LayoutParams p = lp(-2, dp(54));
    p.leftMargin = dp(12);
    row.addView(b, p);
  }

  void renderFavorites() {
    List<AppCatalog.App> favorites = AppCatalog.favorites(this, catalog);
    View breathingRoom = new View(this);
    root.addView(breathingRoom, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout hero = Ui.column(this);
    hero.setGravity(Gravity.CENTER_VERTICAL);
    root.addView(hero, lp(-1, dp(112)));
    TextView titleView = Ui.text(this, "Favorites", 36, Ui.WHITE);
    titleView.setTypeface(Typeface.create("sans-serif-medium", 0));
    hero.addView(titleView);
    HorizontalScrollView scroll = new HorizontalScrollView(this);
    scroll.setClipChildren(false);
    scroll.setClipToPadding(false);
    scroll.setHorizontalScrollBarEnabled(false);
    scroll.setPadding(0, dp(16), 0, dp(10));
    root.addView(scroll, lp(-1, dp(324)));
    LinearLayout row = new LinearLayout(this);
    row.setClipChildren(false);
    row.setClipToPadding(false);
    scroll.addView(row);
    int width =
        Math.max(
            dp(260),
            Math.min(dp(410), (getResources().getDisplayMetrics().widthPixels - dp(152 + 96)) / 4));
    for (AppCatalog.App app : favorites) {
      LinearLayout item = Ui.column(this);
      item.setClipChildren(false);
      LinearLayout.LayoutParams ip = lp(width, -1);
      ip.rightMargin = dp(28);
      row.addView(item, ip);
      Ui.Card card = new Ui.Card(this, app);
      item.addView(card, lp(-1, dp(252)));
      card.setOnClickListener(v -> launch(app));
      card.setOnLongClickListener(
          v -> {
            editFavorite(app);
            return true;
          });
      item.setOnClickListener(v -> launch(app));
      item.setOnLongClickListener(
          v -> {
            editFavorite(app);
            return true;
          });
      TextView label = Ui.text(this, app.name, 20, Ui.WHITE);
      label.setGravity(Gravity.CENTER);
      label.setPadding(0, dp(18), 0, 0);
      label.setMaxLines(1);
      label.setEllipsize(android.text.TextUtils.TruncateAt.END);
      item.addView(label, lp(-1, -2));
    }
    if (favorites.isEmpty()) {
      TextView add = Ui.button(this, "+ Add a favorite", () -> chooseApp(null));
      row.addView(add, lp(width, dp(234)));
    }

    LinearLayout lower = new LinearLayout(this);
    lower.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams lowerP = lp(-1, 0);
    lowerP.height = dp(90);
    lowerP.weight = 0;
    root.addView(lower, lowerP);
    TextView arrange = Ui.button(this, "Edit favorites", () -> editMenu());
    lower.addView(arrange, lp(-2, dp(54)));
    root.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1));
  }

  void renderAll() {
    TextView title = Ui.text(this, "All Apps", 43, Ui.WHITE);
    title.setTypeface(Typeface.create("sans-serif-medium", 0));
    LinearLayout.LayoutParams tp = lp(-1, -2);
    tp.topMargin = dp(38);
    tp.bottomMargin = dp(26);
    root.addView(title, tp);
    ScrollView scroll = new ScrollView(this);
    scroll.setClipToPadding(false);
    scroll.setPadding(dp(8), dp(8), dp(8), dp(8));
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    GridLayout grid = new GridLayout(this);
    int cols = Math.max(2, Math.min(6, getResources().getDisplayMetrics().widthPixels / dp(290)));
    grid.setColumnCount(cols);
    scroll.addView(grid);
    int width = (getResources().getDisplayMetrics().widthPixels - dp(168)) / cols - dp(20);
    for (AppCatalog.App app : catalog) {
      LinearLayout item = Ui.column(this);
      Ui.Card card = new Ui.Card(this, app);
      item.addView(card, lp(-1, dp(132)));
      card.setOnClickListener(v -> launch(app));
      card.setOnLongClickListener(
          v -> {
            editApp(app);
            return true;
          });
      item.setOnClickListener(v -> launch(app));
      item.setOnLongClickListener(
          v -> {
            editApp(app);
            return true;
          });
      TextView label = Ui.text(this, app.name, 17, Ui.WHITE);
      label.setGravity(Gravity.CENTER);
      label.setSingleLine(true);
      label.setEllipsize(android.text.TextUtils.TruncateAt.END);
      label.setPadding(0, dp(12), 0, dp(8));
      item.addView(label);
      GridLayout.LayoutParams p = new GridLayout.LayoutParams();
      p.width = width;
      p.height = dp(188);
      p.setMargins(dp(4), dp(4), dp(16), dp(12));
      grid.addView(item, p);
    }
  }

  void sheet(String title, String subtitle, List<String> labels, List<Runnable> actions) {
    Dialog dialog = new Dialog(this);
    LinearLayout content = Ui.column(this);
    content.setPadding(dp(30), dp(28), dp(30), dp(24));
    content.setBackground(Ui.shape(0xFF171F30, 28, this));
    TextView heading = Ui.text(this, title, 28, Ui.WHITE);
    heading.setTypeface(Typeface.create("sans-serif-medium", 0));
    content.addView(heading);
    if (subtitle != null) {
      TextView detail = Ui.text(this, subtitle, 16, Ui.MUTED);
      detail.setPadding(0, dp(10), 0, dp(10));
      content.addView(detail);
    }
    ScrollView sc = new ScrollView(this);
    LinearLayout list = Ui.column(this);
    sc.addView(list);
    content.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));
    for (int i = 0; i < labels.size(); i++) {
      final int index = i;
      TextView b =
          Ui.button(
              this,
              labels.get(i),
              () -> {
                dialog.dismiss();
                actions.get(index).run();
              });
      LinearLayout.LayoutParams p = lp(-1, dp(58));
      p.topMargin = dp(10);
      list.addView(b, p);
    }
    TextView close = Ui.button(this, "Done", dialog::dismiss);
    LinearLayout.LayoutParams cp = lp(-1, dp(54));
    cp.topMargin = dp(18);
    content.addView(close, cp);
    dialog.setContentView(content);
    Window w = dialog.getWindow();
    if (w != null) {
      w.setBackgroundDrawableResource(android.R.color.transparent);
      w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
      WindowManager.LayoutParams params = w.getAttributes();
      params.dimAmount = .65f;
      w.setAttributes(params);
      w.setLayout(
          dp(580),
          Math.min(
              dp(190 + labels.size() * 68),
              getResources().getDisplayMetrics().heightPixels - dp(120)));
    }
    dialog.show();
    if (w != null)
      w.setLayout(
          dp(580),
          Math.min(
              dp(200 + labels.size() * 68),
              getResources().getDisplayMetrics().heightPixels - dp(120)));
  }

  void editFavorite(AppCatalog.App app) {
    List<String> order = AppCatalog.order(this);
    int index = order.indexOf(app.pkg);
    List<String> labels = new ArrayList<>();
    List<Runnable> acts = new ArrayList<>();
    if (index > 0) {
      labels.add("Move left");
      acts.add(
          () -> {
            Collections.swap(order, index, index - 1);
            AppCatalog.save(this, order);
            render();
          });
    }
    if (index >= 0 && index < order.size() - 1) {
      labels.add("Move right");
      acts.add(
          () -> {
            Collections.swap(order, index, index + 1);
            AppCatalog.save(this, order);
            render();
          });
    }
    labels.add("Replace app…");
    acts.add(() -> chooseApp(app.pkg));
    labels.add("Remove from favorites");
    acts.add(
        () -> {
          order.remove(app.pkg);
          AppCatalog.save(this, order);
          render();
        });
    labels.add("App info");
    acts.add(
        () ->
            open(
                new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + app.pkg))));
    sheet(app.name, "Make this space yours.", labels, acts);
  }

  void editApp(AppCatalog.App app) {
    List<String> order = AppCatalog.order(this);
    boolean contains = order.contains(app.pkg);
    sheet(
        app.name,
        null,
        Arrays.asList(contains ? "Remove from favorites" : "Add to favorites", "App info"),
        Arrays.asList(
            () -> {
              if (contains) order.remove(app.pkg);
              else order.add(app.pkg);
              AppCatalog.save(this, order);
              Toast.makeText(
                      this,
                      contains ? "Removed from favorites" : "Added to favorites",
                      Toast.LENGTH_SHORT)
                  .show();
              render();
            },
            () ->
                open(
                    new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + app.pkg)))));
  }

  void chooseApp(String replacing) {
    List<String> order = AppCatalog.order(this);
    List<String> labels = new ArrayList<>();
    List<Runnable> acts = new ArrayList<>();
    for (AppCatalog.App app : catalog)
      if (!order.contains(app.pkg)) {
        labels.add(app.name);
        acts.add(
            () -> {
              if (replacing != null) {
                int i = order.indexOf(replacing);
                if (i >= 0) order.set(i, app.pkg);
                else order.add(app.pkg);
              } else order.add(app.pkg);
              AppCatalog.save(this, order);
              render();
            });
      }
    sheet(
        replacing == null ? "Add a favorite" : "Replace app",
        "Choose an installed app.",
        labels,
        acts);
  }

  void editMenu() {
    List<AppCatalog.App> favorites = AppCatalog.favorites(this, catalog);
    List<String> labels = new ArrayList<>();
    List<Runnable> acts = new ArrayList<>();
    for (AppCatalog.App app : favorites) {
      labels.add(app.name);
      acts.add(() -> editFavorite(app));
    }
    labels.add("+ Add a favorite");
    acts.add(() -> chooseApp(null));
    sheet("Edit favorites", "Choose an app to move, replace, or remove.", labels, acts);
  }

  void navigationSetup() {
    open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
  }

  void settings() {
    sheet(
        "Settings",
        null,
        Arrays.asList(
            "Updates",
            "Edit favorites",
            "Home tab: reset position",
            "Home tab access",
            "Default home app",
            "Android settings"),
        Arrays.asList(
            () -> Updates.get(this).show(this),
            () -> editMenu(),
            () -> {
              getSharedPreferences("home", 0)
                  .edit()
                  .remove("tab_y")
                  .putBoolean("tab_right", true)
                  .apply();
              Navigation.refresh();
              Toast.makeText(this, "Home tab reset to the right edge", Toast.LENGTH_SHORT).show();
            },
            () -> navigationSetup(),
            () -> open(new Intent(Settings.ACTION_HOME_SETTINGS)),
            () -> open(new Intent(Settings.ACTION_SETTINGS))));
  }

  @Override
  public void onBackPressed() {
    if (all) {
      all = false;
      render();
    }
  }
}
