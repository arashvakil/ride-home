package nyc.arash.ridehome;

import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

final class Ui {
  static final int WHITE = 0xFFF5F6FB,
      MUTED = 0xFFAAB3C7,
      SURFACE = 0xFF202737,
      ACCENT = 0xFFDCE7FF;

  static int dp(Context c, float n) {
    return Math.round(n * c.getResources().getDisplayMetrics().density);
  }

  static GradientDrawable shape(int color, int radius, Context c) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(c, radius));
    return d;
  }

  static TextView text(Context c, String s, int size, int color) {
    TextView v = new TextView(c);
    v.setText(s);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setFontFeatureSettings("kern");
    return v;
  }

  static void feedback(View v) {
    v.setFocusable(true);
    v.setClickable(true);
    v.setOnFocusChangeListener(
        (w, f) ->
            w.animate().scaleX(f ? 1.035f : 1).scaleY(f ? 1.035f : 1).setDuration(160).start());
    v.setOnTouchListener(
        (w, e) -> {
          if (e.getActionMasked() == MotionEvent.ACTION_DOWN)
            w.animate().scaleX(.97f).scaleY(.97f).setDuration(90).start();
          if (e.getActionMasked() == MotionEvent.ACTION_UP
              || e.getActionMasked() == MotionEvent.ACTION_CANCEL)
            w.animate()
                .scaleX(w.hasFocus() ? 1.035f : 1)
                .scaleY(w.hasFocus() ? 1.035f : 1)
                .setDuration(160)
                .start();
          return false;
        });
  }

  static TextView button(Context c, String label, Runnable action) {
    TextView v = text(c, label, 17, WHITE);
    v.setGravity(Gravity.CENTER);
    v.setPadding(dp(c, 22), 0, dp(c, 22), 0);
    v.setMinHeight(dp(c, 54));
    GradientDrawable normal = shape(SURFACE, 27, c);
    StateListDrawable states = new StateListDrawable();
    GradientDrawable focused = shape(0xFF3C4C68, 27, c);
    focused.setStroke(dp(c, 2), 0xFFB8CAEE);
    states.addState(new int[] {android.R.attr.state_focused}, focused);
    states.addState(new int[] {}, normal);
    v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x448DAAFF), states, null));
    feedback(v);
    v.setOnClickListener(w -> action.run());
    return v;
  }

  static LinearLayout column(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  static class Backdrop extends View {
    int color = 0xFF384D82;
    Paint p = new Paint(3);

    Backdrop(Context c) {
      super(c);
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas c) {
      float w = getWidth(), h = getHeight();
      c.drawColor(0xFF0B101B);
      p.setColor(Color.WHITE);
      p.setShader(
          new RadialGradient(
              w * .76f,
              h * .18f,
              w * .75f,
              new int[] {(color & 0xFFFFFF) | 0x90000000, 0x00000000},
              null,
              Shader.TileMode.CLAMP));
      c.drawRect(0, 0, w, h, p);
      p.setShader(null);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(dp(getContext(), 1));
      p.setColor(0x08FFFFFF);
      for (int i = 0; i < 3; i++)
        c.drawCircle(w * .88f, h * .22f, dp(getContext(), 170 + i * 100), p);
      p.setStyle(Paint.Style.FILL);
    }
  }

  static class Card extends View {
    final AppCatalog.App app;
    Paint p = new Paint(3);
    Drawable icon;

    Card(Context c, AppCatalog.App a) {
      super(c);
      app = a;
      icon = a.info.loadIcon(c.getPackageManager()).mutate();
      setContentDescription(a.name);
      setFocusable(true);
      setClickable(true);
      setElevation(dp(c, 8));
      setFocusable(false);
      setOnTouchListener(
          (v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN)
              animate().scaleX(.975f).scaleY(.975f).setDuration(90).start();
            if (e.getActionMasked() == MotionEvent.ACTION_UP
                || e.getActionMasked() == MotionEvent.ACTION_CANCEL)
              animate().scaleX(1).scaleY(1).setDuration(160).start();
            return false;
          });
    }

    @Override
    protected void onDraw(Canvas c) {
      float w = getWidth(), h = getHeight(), r = dp(getContext(), 23);
      RectF bounds = new RectF(3, 3, w - 3, h - 3);
      Path clip = new Path();
      clip.addRoundRect(bounds, r, r, Path.Direction.CW);
      c.save();
      c.clipPath(clip);
      p.setColor(Color.WHITE);
      p.setShader(
          new LinearGradient(
              0, 0, w, h, new int[] {app.color, 0xFF111725}, null, Shader.TileMode.CLAMP));
      c.drawRect(bounds, p);
      p.setShader(
          new RadialGradient(
              w * .6f,
              h * .25f,
              w * .65f,
              new int[] {0x25FFFFFF, 0x00FFFFFF},
              null,
              Shader.TileMode.CLAMP));
      c.drawRect(bounds, p);
      p.setShader(null);
      p.setColor(0x0DFFFFFF);
      c.drawCircle(w * 1.05f, h * 1.1f, w * .65f, p);
      p.setColor(0x08FFFFFF);
      c.drawCircle(w * .95f, h * 1.2f, w * .86f, p);
      int size = (int) Math.min(h * .48f, w * .40f);
      int left = (int) (w - size) / 2, top = (int) (h - size) / 2;
      icon.setBounds(left, top, left + size, top + size);
      icon.draw(c);
      c.restore();
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(dp(getContext(), isPressed() ? 3 : 1));
      p.setColor(isPressed() ? 0xFFE8EEFF : 0x24FFFFFF);
      c.drawRoundRect(bounds, r, r, p);
      p.setStyle(Paint.Style.FILL);
    }
  }
}
