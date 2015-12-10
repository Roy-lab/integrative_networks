package filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import structures.Feature;
import structures.Graph;
import structures.Value;

/**
 * FilterManagers: need items, filters, things for looking up item values.
 * @author chasman
 *
 */
public abstract class FilterManager<T, F extends Filter, L> {

	public static final String DELIM="\\|";
	
	/**
	 * How to apply a set of filters to a single T:
	 * - AND: all filters must accept
	 * - OR: any filter can accept
	 * - NONE: Don't run the filter - everything accepted
	 * (Apply to all edge or node filters separately)
	 * @author chasman
	 *
	 */
	public static enum FilterItemMode {
		AND, // Must be accepted by ALL filters
		OR, // Must be accepted by at least one filter
		NONE;	// Don't run filter
		
	}
	
	/**
	 * How to apply one or more filters to a set of items.
	 * Set the filter mode separately.
	 * - ALL: Set accepted if all single items accepted
	 * - ANY: Set accepted if any single item accepted
	 * - XOR: Set accepted if only one item accepted
	 * - AT_MOST_ONE: Set accepted if no more than one item accepted.
	 * - NONE: Set always accepted. (no-op)
	 * @author chasman
	 *
	 */
	public static enum FilterSetMode {
		ALL,	// Set accepted if ALL accepted
		ANY,		// Set accepted if ANY accepted
		XOR,	// Set accepted if ONLY ONE accepted
		AT_MOST_ONE, // Set accepted if AT MOST ONE accepted
		NONE;	// Don't run the filter; accept all.
	}	
	
	protected FilterItemMode itemMode;
	protected FilterSetMode setMode;
	protected ArrayList<F> filters;
	protected String name;
	
	protected L library;
	
	/**
	 * Instantiates stuff. (to reduce duplicate code in other constructors)
	 * @param itemMode
	 * @param setMode
	 */
	protected FilterManager(String name,  
			FilterItemMode itemMode, FilterSetMode setMode,
			L library) {
		this.itemMode=itemMode;
		this.setMode=setMode;
		this.name=name;
		this.library=library;
	}
	
	/**
	 * Constructs a FilterManager with a single filter, a library,
	 * and the filter modes.
	 * @param filter
	 * @param library
	 */
	protected FilterManager(String name, F filter,  
			FilterItemMode itemMode, FilterSetMode setMode, L library) {
		this(name, itemMode, setMode, library);
		this.filters=new ArrayList<F>();
		filters.add(filter);	
	}
	
	/**
	 * @param name
	 * @param filters
	 * @param library
	 * @param itemMode
	 * @param setMode
	 */
	protected FilterManager(String name, Collection<F> filters, 
			FilterItemMode itemMode, FilterSetMode setMode, L library) {
		this(name, itemMode, setMode, library);
		this.filters = new ArrayList<F>(filters);
	}
	
	public FilterItemMode itemMode() {
		return this.itemMode;
	}
	
	public FilterSetMode setMode() {
		return this.setMode;
	}
	
	public String name() {
		return this.name;
	}
	
	public void setLibrary(L library) {
		this.library=library;
	}
	
	public L library() {
		return this.library;
	}
	
	/**
	 * Need to be able to get a value for a given feature and item.
	 * This will come out of the "library" - node library or graph.
	 * This is what's notably missing from the base filter manager.
	 * @param item
	 * @param f
	 * @return
	 */
	protected abstract Value getValue(T item, Feature f);
	
	/**
	 * Apply the filter manager to a graph.
	 * @param g
	 * @return
	 */
	public abstract Graph filter(Graph g);
	
	/**
	 * Does the filter manager accept this node?
	 * Depends on filter mode.
	 */
	public boolean accept(T item) {
		assert(this.library!=null) : "Need to give a library to the filter manager.";
		
		boolean accept=true;
		switch(this.itemMode) {
		case AND: accept=acceptAnd(item); break;
		case OR: accept=acceptOr(item); break;
		default: accept=true;	// otherwise, the filter manager is a no-op
		}
		return accept;
	}
	
	/**
	 * Accept set of nodes if the individual items are accepted
	 *  in coordination with the filter modes.
	 */
	public boolean accept(Collection<T> items) {
		// test each item against all filters
		boolean accept=true;
		
		switch(this.setMode) {
		case ANY: accept=acceptAny(items); break;
		case ALL: accept=acceptAll(items); break;
		case XOR: accept=acceptXor(items); break;
		case AT_MOST_ONE: accept=acceptAtMostOne(items); break;
		default: accept=true;
		}	
		return accept;		
	}
	
	/**
	 * Returns true if all items accepted according to the FilterItemMode.
	 * @param nodes
	 * @return
	 */
	protected boolean acceptAll(Collection<T> items) {
		for (T item : items) {
			// stop immediately if one fails
			if (!this.accept(item)) return false;
		}
		return true;
	}
	
	/**
	 * Returns true if only one item accepted according to the FilterItemMode.
	 * @param nodes
	 * @return
	 */
	protected boolean acceptXor(Collection<T> items) {
		int accepted=0;
		for (T item : items) {
			if (this.accept(item)) accepted++;			
		}
		return (accepted==1);
	}
	
	/**
	 * Returns true if at most one item accepted according to the FilterItemMode.
	 * @param nodes
	 * @return
	 */
	protected boolean acceptAtMostOne(Collection<T> items) {
		int accepted=0;
		for (T item : items) {
			if (this.accept(item)) accepted++;			
		}
		return (accepted<2);
	}
	
	/**
	 * Returns true once the first accepted node is found.
	 * @param nodes
	 * @return
	 */
	protected boolean acceptAny(Collection<T> items) {
		for (T item : items) {
			// return immediately if one accepted
			if (this.accept(item)) return true;
		}
		return false;
	}

	
	/**
	 * Runs all filters on this node; returns true if all filters
	 * accept.
	 * @param node
	 * @return
	 */
	protected boolean acceptAnd(T item) {
		for (F f : this.filters) {
			Value v = this.getValue(item, f.feature());
			if (!f.accept(v)) 
				return false;
		}
		return true;
	}
	
	/**
	 * Runs all filters on this node; returns true once
	 * any filter accepts it.
	 * @param node
	 * @return
	 */
	protected boolean acceptOr(T item) {
		for (F f : this.filters) {
			Value v = this.getValue(item, f.feature());
			if (f.accept(v)) {
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Applies the filters individually to each item in a collection;
	 * returns a list o those that pass the filter.
	 */
	public List<T> apply(Collection<T> items) {
		ArrayList<T> accepted = new ArrayList<T>();
		for (T item : items) {
			if (this.accept(item)) {
				accepted.add(item);
			}
		}
		return accepted;
	}

	
}
