package nyc.arash.ridehome;

import java.util.*;

/** Preference encoding, independent of Android so real migrations can be tested. */
final class FavoriteOrder {
  private FavoriteOrder() {}

  static List<String> read(String saved, Set<String> legacy) {
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    if (saved != null) {
      for (String item : saved.split("\\|")) if (!item.isEmpty()) unique.add(item);
    } else if (legacy != null) {
      List<String> sorted = new ArrayList<>(legacy);
      Collections.sort(sorted);
      unique.addAll(sorted);
    }
    return new ArrayList<>(unique);
  }
}
