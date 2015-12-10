package structures;
import java.util.HashMap;



/**
 * Wrapper for string values, implemented with Singleton pattern
 * @author chasman
 *
 */
public class Discrete extends Value {
	private String value; 
	
	private final static HashMap<String, Discrete> created = 
		new HashMap<String, Discrete>();

	private Discrete(String s) {
		this.value = s;
	}
	public String getValue() {
		return value;
	}
	public Type getType() {
		return Type.DISCRETE;
	}
	
	public static Discrete makeValue(String value) {
		if (!created.containsKey(value)) {
			Discrete disc = new Discrete(value);
			created.put(value, disc);
		}
		return created.get(value);		
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
		if (!(other instanceof Discrete)) return false;
		return (this.toString().equals(other.toString()));
	}
	
	@Override
	public int compareTo(Value o) {
		return this.toString().compareTo(o.toString());
	}
}
