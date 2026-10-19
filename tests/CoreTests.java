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
    // Trust boundary: exact release identity, transport hosts, signer set, newer version.
    String hash = String.join("", Collections.nCopies(64, "a"));
    String url =
        "https://github.com/arashvakil/ride-home/releases/download/v2.2.0/RideHome-2.2.0.apk";
    same(true, UpdatePolicy.manifest(6, "2.2.0", url, hash, UpdatePolicy.PACKAGE));
    same(false, UpdatePolicy.manifest(0, "2.2.0", url, hash, UpdatePolicy.PACKAGE));
    same(false, UpdatePolicy.manifest(6, "../../bad", url, hash, UpdatePolicy.PACKAGE));
    same(
        false,
        UpdatePolicy.manifest(
            6, "2.2.0", url.replace("ride-home", "other"), hash, UpdatePolicy.PACKAGE));
    same(false, UpdatePolicy.manifest(6, "2.2.0", url, "bad", UpdatePolicy.PACKAGE));
    same(false, UpdatePolicy.manifest(6, "2.2.0", url, hash, "other.package"));
    same(true, UpdatePolicy.secureTransport(UpdatePolicy.CHANNEL));
    same(
        true,
        UpdatePolicy.secureTransport(
            "https://release-assets.githubusercontent.com/signed?token=one"));
    for (String bad :
        Arrays.asList(
            "http://github.com/file",
            "https://evil.test/file",
            "https://github.com.evil.test/file",
            "https://user:pass@github.com/file",
            "https://github.com:8443/file",
            "file:///tmp/update.apk",
            "https://github.com/file#fragment",
            "garbage")) same(false, UpdatePolicy.secureTransport(bad));
    Set<String> signer = new HashSet<>(Arrays.asList("cert"));
    same(true, UpdatePolicy.apk("app", 5, signer, "app", 6, signer, 6));
    same(false, UpdatePolicy.apk("app", 5, signer, "other", 6, signer, 6));
    same(false, UpdatePolicy.apk("app", 5, signer, "app", 5, signer, 5));
    same(false, UpdatePolicy.apk("app", 5, signer, "app", 6, signer, 7));
    same(
        false,
        UpdatePolicy.apk("app", 5, signer, "app", 6, new HashSet<>(Arrays.asList("other")), 6));
    same(
        false,
        UpdatePolicy.apk("app", 5, Collections.emptySet(), "app", 6, Collections.emptySet(), 6));
    same(
        false,
        UpdatePolicy.apk(
            "app", 5, signer, "app", 6, new HashSet<>(Arrays.asList("cert", "extra")), 6));
    same(true, UpdatePolicy.due(100, 0));
    same(true, UpdatePolicy.due(100, 101));
    same(false, UpdatePolicy.due(1000, 100));
    same(true, UpdatePolicy.due(21600100, 100));
    System.out.println("Passed " + checks + " core regression checks.");
  }
}
