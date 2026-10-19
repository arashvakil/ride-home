package nyc.arash.ridehome;

import java.util.*;

public final class CoreTests {
  static int checks;

  static void same(Object expected, Object actual) {
    checks++;
    if (!expected.equals(actual))
      throw new AssertionError("Expected " + expected + ", got " + actual);
  }

  public static void main(String[] args) {
    // A clean installation should not expose the author's own app selection.
    same(Collections.emptyList(), FavoriteOrder.read(null, null));
    same(Collections.emptyList(), FavoriteOrder.read("", null));
    same(Arrays.asList("app.b", "app.a"), FavoriteOrder.read("app.b|app.a|app.b||", null));
    same(
        Arrays.asList("app.a", "app.b"),
        FavoriteOrder.read(null, new HashSet<>(Arrays.asList("app.b", "app.a"))));
    same(
        Arrays.asList("app.b"), FavoriteOrder.read("app.b", new HashSet<>(Arrays.asList("app.a"))));
    // Full-screen drag, edge snapping, and persistence across resized displays.
    same(0, NavGeometry.edgeX(1920, 108, 0));
    same(1812, NavGeometry.edgeX(1920, 108, 1812));
    same(0, NavGeometry.edgeX(1920, 108, 500));
    same(1812, NavGeometry.edgeX(1920, 108, 1000));
    same(0, NavGeometry.edgeX(80, 108, 100));
    same(28, NavGeometry.restoreY(-1, 1080, 52, 28, 16));
    same(1012, NavGeometry.restoreY(2, 1080, 52, 28, 16));
    same(518, NavGeometry.restoreY(.48f, 1080, 52, 28, 16));
    same(259, NavGeometry.restoreY(.48f, 540, 52, 28, 16));
    same(518, NavGeometry.restoreY(Float.NaN, 1080, 52, 28, 16));
    same(518, NavGeometry.restoreY(Float.POSITIVE_INFINITY, 1080, 52, 28, 16));
    same(28, NavGeometry.restoreY(.5f, 40, 52, 28, 16));
    same(0, NavGeometry.clamp(-10, 0, 1812));
    same(1812, NavGeometry.clamp(99999, 0, 1812));
    System.out.println("Passed " + checks + " core regression checks.");
  }
}
