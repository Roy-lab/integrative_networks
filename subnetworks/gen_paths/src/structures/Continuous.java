package structures;

import java.util.HashMap;

/**
 * Wrapper for double values, implemented with Singleton pattern
 * @author chasman
 *
 */
public class Continuous extends Value {
	
	private double value; 
	
	// Keeps Singletons of the values we've created so far.
	private static HashMap<String, Continuous> created = new HashMap<String, Continuous>();

	private Continuous(String s) throws NumberFormatException {
		this.value = Double.parseDouble(s);
	}
	private Continuous(double d) {
		value = d;
	}
	
	public double getValue() {
		return value;
	}
	
	public Type getType() {
		return Type.CONTINUOUS;
	}
	
	/**
	 * Either makes a continuous out of the provided double,
	 * or retrieves the one we've made previously 
	 * @param d
	 * @return
	 */
	public static Continuous makeValue(String ds) {
		if (!Continuous.created.containsKey(ds)) {
			Continuous cont = new Continuous(Double.parseDouble(ds));
			created.put(ds, cont);
		}
		return Continuous.created.get(ds);
	}
	
	public static Continuous makeValue(double d) {
		String ds = Double.toString(d);
		return makeValue(ds);
	}
	
	@Override
	public String toString() {
		return String.valueOf(value);
	}		
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Continuous)) return false;
		return (this.toString().equals(other.toString()));
	}	

	public int compareTo(Continuous other) {
		return Double.compare(this.value, other.value);
	}
	
	/**
	 * If compared to another Continuous, compare the values.
	 * Otherwise, compare strings.
	 */
	public int compareTo(Value other) {
		if (other instanceof Continuous) {
			return this.compareTo((Continuous) other);
		} else {
			return (this.toString().compareTo(other.toString()));
		}
	}
}
