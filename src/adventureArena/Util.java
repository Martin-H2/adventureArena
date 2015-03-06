package adventureArena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Util {

	public static <K, V extends Comparable<V>> List<Entry<K, V>> sortByValue(final Map<K, V> map, final boolean ascending) {
		List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>(map.entrySet());
		Comparator<Entry<K, V>> c = ascending ? new ByValueAsc<K, V>() : new ByValueDesc<K, V>();
		Collections.sort(entries, c);
		return entries;
	}

	private static class ByValueAsc<K, V extends Comparable<V>> implements Comparator<Entry<K, V>> {
		public int compare(final Entry<K, V> o1, final Entry<K, V> o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
	}
	private static class ByValueDesc<K, V extends Comparable<V>> implements Comparator<Entry<K, V>> {
		public int compare(final Entry<K, V> o1, final Entry<K, V> o2) {
			return -o1.getValue().compareTo(o2.getValue());
		}
	}






}
