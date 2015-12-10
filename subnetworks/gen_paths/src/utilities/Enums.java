package utilities;

public class Enums {
	
	/**
	 * Which edges from a subgraph should be included in the IP input?
	 * @author chasman
	 *
	 */
	public enum AddEdgeMode {
		ALL,	// Include all edges 
		TARGET_IN_PATH, 	// Include edge only if its target is in a path
		SOURCE_IN_PATH; // Include edge only if its source is in a path
	}

	public enum Sign {
		POSITIVE(1),
		NEGATIVE(-1),
		UNKNOWN(0);

		private int val;
		private Sign(int val) {
			this.val=val;
		}

		public int value() {
			return this.val;
		}

		/**
		 * Returns a one-character abbreviation
		 * for the value of the sign.
		 * @return
		 */
		public String abbrev() {
			switch (this) {
			case POSITIVE: return "a";
			case NEGATIVE: return "h";
			case UNKNOWN: return "u";
			default:	return "u";
			}
		}

		public static Sign fromValue(int val) {
			if (val==0) return UNKNOWN;
			else if (val > 0) return POSITIVE;
			else return NEGATIVE;
		}	

	}

}
