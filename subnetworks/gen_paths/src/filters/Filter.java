package filters;

import structures.Feature;
import structures.Value;
import exceptions.IncomparableException;
import exceptions.InvalidValueException;

/**
 * Here so I can define anything that filters values.
 * @author chasman
 *
 * 
 */
public abstract class Filter {
	
	// Implemented filters
	public enum FilterType {

		ORDERED("OrderedFilter"),
		EQUALS("EqualsFilter"),
		NOT("NotEqualsFilter");

		private String className;

		private FilterType(String name) {
			this.className=name;
		}

		public static FilterType byName(String name) {
			for (FilterType t : FilterType.values()) {
				if (name.equals(t.className)) return t;
			}
			return null;
		}
		public String className() {
			return this.className;
		}
	}
	
	protected final String name;
	protected final Feature feature;	
	protected final boolean acceptNull;	// Should we accept 'null' values for features?
	
	protected Filter(String name, Feature feat, boolean acceptNull) {
		this.name=name;
		this.feature=feat;
		this.acceptNull=acceptNull;
	}
	
	public String name() {
		return this.name;
	}
	
	public Feature feature() {
		return this.feature;
	}
	
	public abstract boolean accept(Value v);	
	
	/**
	 * Makes a filter from a line in a config file.
	 * (for example: NFILTER	name	OrderedNodeFilter	cont	0.25	greater)
	 * @param line
	 * @param libe
	 * @return
	 */
	public static Filter makeFilter(String[] line, Feature f) 
	throws InvalidValueException, IncomparableException {
		// which version of Filter? 
		FilterType type = FilterType.byName(line[2]);
		if (type==null) {
			throw new InvalidValueException("Invalid filter type: " + line[2]);
		}
		
		Filter filter=null;
		switch(type) {
		case ORDERED: filter = OrderedFilter.makeFilter(line, f); break;
		case EQUALS: filter = EqualsFilter.makeFilter(line, f); break;
		case NOT: filter = NotEqualsFilter.makeFilter(line, f); break;
		default: filter=null;
		}

		return filter;
	}
	
}
