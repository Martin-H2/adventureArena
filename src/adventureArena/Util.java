package adventureArena;

import java.util.*;
import java.util.Map.Entry;

public class Util {

	public static <K, V extends Comparable<V>> List<Entry<K, V>> sortByValue(final Map<K, V> map, final boolean ascending) {
		List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>(map.entrySet());
		Comparator<Entry<K, V>> c = ascending ? new ByValueAsc<K, V>() : new ByValueDesc<K, V>();
		Collections.sort(entries, c);
		return entries;
	}

	private static class ByValueAsc<K, V extends Comparable<V>> implements Comparator<Entry<K, V>> {

		@Override
		public int compare(final Entry<K, V> o1, final Entry<K, V> o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
	}

	private static class ByValueDesc<K, V extends Comparable<V>> implements Comparator<Entry<K, V>> {

		@Override
		public int compare(final Entry<K, V> o1, final Entry<K, V> o2) {
			return -o1.getValue().compareTo(o2.getValue());
		}
	}



	//	public static <E extends Enum<E>> E getEnumStartingWith(String prefix, E[] myEnum) {
	//		prefix = prefix.toUpperCase();
	//
	//		for (E m: myEnum) {
	//			if (m.name().startsWith(prefix)) return m;
	//		}
	//		return null;
	//	}

	public static <E extends Enum<E>> E getEnumStartingWith(String prefix, Class<E> enumClass) {
		prefix = prefix.toUpperCase();
		try {
			return Enum.valueOf(enumClass, prefix);
		}
		catch (IllegalArgumentException e) {
			for (E enumConstant: enumClass.getEnumConstants()) {
				if (enumConstant.name().startsWith(prefix)) return enumConstant;
			}
			return null;
		}
	}



}
