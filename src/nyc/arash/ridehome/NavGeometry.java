package nyc.arash.ridehome;

/** Geometry shared by the floating tab and its regression tests. */
final class NavGeometry {
  private NavGeometry() {}

  static int clamp(int value, int minimum, int maximum) {
    return Math.max(minimum, Math.min(value, Math.max(minimum, maximum)));
  }

  static int edgeX(int screenWidth, int tabWidth, int x) {
    int available = Math.max(0, screenWidth - tabWidth);
    return x + tabWidth / 2 < screenWidth / 2 ? 0 : available;
  }

  static int restoreY(float fraction, int height, int tabHeight, int top, int bottom) {
    if (Float.isNaN(fraction) || Float.isInfinite(fraction)) fraction = .48f;
    return clamp(Math.round(fraction * height), top, height - tabHeight - bottom);
  }
}
