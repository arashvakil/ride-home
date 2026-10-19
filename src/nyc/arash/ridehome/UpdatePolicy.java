package nyc.arash.ridehome;

import java.net.URI;
import java.util.Set;

/** Pure update identity and transport policy, shared by the downloader and tests. */
final class UpdatePolicy {
  static final String PACKAGE = "nyc.arash.ridehome";
  static final String CHANNEL =
      "https://github.com/arashvakil/ride-home/releases/latest/download/update.json";

  static boolean secureTransport(String value) {
    try {
      URI uri = new URI(value);
      String host = uri.getHost();
      return "https".equals(uri.getScheme())
          && uri.getUserInfo() == null
          && (uri.getPort() == -1 || uri.getPort() == 443)
          && uri.getFragment() == null
          && ("github.com".equals(host)
              || "release-assets.githubusercontent.com".equals(host)
              || "objects.githubusercontent.com".equals(host));
    } catch (Exception e) {
      return false;
    }
  }

  static boolean manifest(int version, String name, String url, String hash, String pkg) {
    return version > 0
        && name != null
        && name.matches("[0-9]+\\.[0-9]+\\.[0-9]+")
        && PACKAGE.equals(pkg)
        && hash != null
        && hash.matches("[a-fA-F0-9]{64}")
        && ("https://github.com/arashvakil/ride-home/releases/download/v"
                + name
                + "/RideHome-"
                + name
                + ".apk")
            .equals(url);
  }

  static boolean apk(
      String installedPackage,
      long installedVersion,
      Set<String> installedSigners,
      String candidatePackage,
      long candidateVersion,
      Set<String> candidateSigners,
      long expected) {
    return installedPackage.equals(candidatePackage)
        && candidateVersion == expected
        && candidateVersion > installedVersion
        && !installedSigners.isEmpty()
        && installedSigners.equals(candidateSigners);
  }

  static boolean due(long now, long previous) {
    return previous <= 0 || previous > now || now - previous >= 6L * 60 * 60 * 1000;
  }
}
