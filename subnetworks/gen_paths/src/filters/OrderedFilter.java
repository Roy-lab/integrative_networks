package filters;

import structures.Feature;
import structures.Value;
import exceptions.IncomparableException;
import exceptions.InvalidValueException;

/**
 * Accepts or rejects nodes based on a comparison
 * of their value for a particular feature to a threshold value.
 * 
 * Works for continuous or ordinal values. 
 * @author chasman
 *
 */
public class OrderedFilter extends Filter {

	// Implemement GE and LE??
	public enum Order { 
		LESSER(-1), 
		EQUAL(0), 
		GREATER(1);		
		private final int resVal;

		private Order(int res) {
			this.resVal=res;
		}

		public int value() {
			return this.resVal;
		}
	}
	
	protected final Value threshold;	
	protected final Order order;	
	
	protected OrderedFilter(String name, Feature f, boolean acceptNull, Value threshold, Order order) {
		super(name, f, acceptNull);
		this.threshold=threshold;	
		this.order=order;
	}
	
	public Value threshold() {
		return this.threshold;
	}
	
	public Order order() {
		return this.order;
	}

	// ???
//	public boolean accept(String node, NodeLibrary libe) {
//		Value v = libe.getValue(node, this.feature);
//		if (v==null) return false;
//		return (v.compareTo(threshold) == order.value());
//	}
	
	/**
	 * Tests out a value for a given node.
	 * @param node
	 * @param val
	 * @return
	 */
	@Override
	public boolean accept(Value v) {
		// is the value legal for this feature?
		if (this.acceptNull && v==null) {
			return true;
		}		
		if (v==null || this.feature.legal(v.toString())==null) {
			return false;
		}
		return (v.compareTo(threshold) == order.value());
	}
	
	public String toString() {
		String acceptNull = this.acceptNull ? "|null" : "";
		return String.format("%s %s %s%s", feature.name(), order.toString(), threshold.toString(), acceptNull);
	}

	/**
	 * @param f	the feature in question
	 * @param threshold	a threshold value for the acceptance decision
	 * @param order	the order that an accepted node must have with the threshold
	 */
	public static OrderedFilter makeFilter(String name, Feature f, boolean acceptNull, Value threshold, Order order) 
	throws InvalidValueException, IncomparableException {
		if (!f.isComparable()) {
			throw new IncomparableException(
					String.format("Cannot base OrderedFilter on feature %s: not comparable.", f.name()));
		}
		if (threshold==null || f.legal(threshold.toString())==null) {
			throw new InvalidValueException(
					String.format("In OrderedFilter(), cannot combine feature %s and value %s.", f.name(), threshold.toString()));
		}		
		return new OrderedFilter(name, f, acceptNull, threshold, order);
	}

	/**
	 * Builds an OrderedNodeFilter from a config file line (and a NodeLibrary).
	 * Line example: NFILTER	OrderedNodeFilter	cont	0.25	greater
	 * There must be a feature named "cont" in the NodeLibrary.
	 * If you declare the feature as "0.25|null" or "null|0.25", then the filter
	 * will also accept nodes for which the feature is not declared.
	 * 
	 * @param line
	 * @return
	 */
	public static OrderedFilter makeFilter(String[] line, Feature f) 
	throws InvalidValueException, IncomparableException {
		if (line.length < 6) throw new InvalidValueException("OrderedNodeFilter not declared correctly.");
		
		String name = line[1];
		//Feature f = libe.getFeature(line[3]);
		
		// accept null?
		boolean acceptNull = line[4].contains("null");
		String valString = line[4];
		if (acceptNull) {
			valString = valString.replace("|null", "").replace("null|", "").replace("null", "");
		}
		
		Value v = f.legal(valString);
		Order o = Order.valueOf(line[5].toUpperCase());
		
		// the other maker-method will test for legality
		return OrderedFilter.makeFilter(name, f, acceptNull, v, o);
	}

}


