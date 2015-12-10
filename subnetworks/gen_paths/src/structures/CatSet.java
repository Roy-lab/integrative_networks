package structures;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import utilities.StringUtils;



/**
 * Wrapper for a set of discrete values, implemented as
 * singleton.
 * @author chasman
 *
 */
public class CatSet extends Value {
	// treeset keeps it sorted
	private TreeSet<Discrete> vals;
	
	private static HashMap<String, CatSet> created = new HashMap<String, CatSet>();
	
	private CatSet(Collection<Discrete> vals) {
		this.vals=new TreeSet<Discrete>();
		for (Discrete v : vals) {
			this.vals.add(v);
		}
	}
	
	public Set<Discrete> getValue() {
		return Collections.unmodifiableSet(this.vals);
	}
	
	public Type getType() {
		return Type.SET;
	}
	
	/**
	 * Method for creating a new catset or retrieving
	 * one that's already been made.
	 * Since we assume the treeset keeps our items sorted,
	 * we compare sets based on their string representation.
	 * @param vals
	 * @return
	 */
	public static CatSet makeValue(Collection<Discrete> vals) {
		CatSet cat = new CatSet(vals);
		String catstr = cat.toString();
		if (!created.containsKey(catstr)) {
			created.put(catstr, cat);
		} 
		return created.get(catstr);
	}
	
	/**
	 * Gets the intersection with another CatSet.
	 * @param other
	 * @return
	 */
	public Set<Discrete> intersection(CatSet other) {
		TreeSet<Discrete> shared = new TreeSet<Discrete>(this.getValue());
		shared.retainAll(other.getValue());
		return shared;
	}
	
	@Override
	public String toString() {		
		return StringUtils.join(this.vals, "|");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof CatSet)) return false;
		return (this.toString().equals(other.toString()));
	}	
	
	public int compareTo(CatSet o) {
		return this.toString().compareTo(o.toString());		
	}
	
	public int compareTo(Value o) {
		return this.toString().compareTo(o.toString());
	}
	
}

