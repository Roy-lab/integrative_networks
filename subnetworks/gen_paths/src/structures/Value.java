package structures;


/**
 * Attribute value
 * @author chasman
 *
 */

public abstract class Value implements Comparable<Value> {
		public enum Type {						
			DISCRETE(false), 	// Single categorical value
			CONTINUOUS(true), // Single floating point value
			ORDINAL(true),	// strings sorted by provided values
			SET(false); // Set of categorical values
			
			// True if individual values can be compared to each other
			// in a meaningful way.
			private final boolean ordered;
			
			private Type(boolean isOrdered) {
				this.ordered=isOrdered;
			}
			
			/**
			 * Returns true if values of this type have a natural order.
			 * @return
			 */
			public boolean isComparable() {
				return this.ordered;
			}
			
			public static Type fromString(String type) {
				type=type.toUpperCase();
				if (type.equals("DISCRETE")) {
					return DISCRETE;
				} else if (type.equals("CONTINUOUS")) {
					return CONTINUOUS;
				} else if (type.equals("SET") || type.equals("CATSET")) {
					return SET;
				} else if (type.equals("ORDINAL")) {
					return ORDINAL;
				}
				return null;
			}
			public String toString() {
				if (this==DISCRETE) {
					return "discrete";
				} else if (this==CONTINUOUS) {
					return "continuous";
				} else if (this==SET) {
					return "categorical_set";
				} else if (this==ORDINAL) {
					return "ordinal";
				}
				return null;
			}
		}		
		
		public abstract Type getType();
		
		public static Value[] convert(Type type, String[] valStrs) {
			int len=valStrs.length;
			Value[] allVals = new Value[len];

			// set values are discrete
			for (int i = 0; i < len; i++) {
				if (type == Value.Type.CONTINUOUS) {
					allVals[i] = Continuous.makeValue(valStrs[i]);
				} else if (type == Value.Type.DISCRETE || type == Value.Type.SET) {
					allVals[i] = Discrete.makeValue(valStrs[i]);
				} else if (type == Value.Type.ORDINAL) {
					allVals[i] = Ordinal.makeValue(valStrs[i], i);
					assert(false):"Ordinal value reading notimplemented.";
				}
			}
			return allVals;
		}
}