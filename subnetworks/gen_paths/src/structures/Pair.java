package structures;

/**
 * Generic container of a pair of objects.
 * 
 * @author chasman
 *
 * @param <K>	item1 type
 * @param <V>	item2 type
 */
public final class Pair<K,V> {
	private final K item1;
	private final V item2;
	
	public Pair(K k, V v) {
		item1=k;
		item2=v;
	}
	
	public K first() {
		return item1;
	}
	
	public V second() {
		return item2;
	}
	
}
