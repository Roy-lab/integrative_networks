package structures;

import java.util.HashMap;

class Ordinal extends Value {
	private static HashMap<Pair<String, Integer>, Ordinal> created=new HashMap<Pair<String,Integer>, Ordinal>();
	
	private final String value;
	private final int ord;
	
	private Ordinal(String val, int ord) {
		this.value=val;
		this.ord=ord;
	}
	
	public String value() {
		return value;
	}
	
	public int order() {
		return ord;
	}

	public int compareTo(Ordinal o) {
		return Double.compare(this.order(), o.order());			
	}
	
	@Override
	public int compareTo(Value other) {
		if (other instanceof Ordinal) {
			return this.compareTo((Ordinal) other);
		} else {
			return (this.toString().compareTo(other.toString()));
		}			
	}

	@Override
	public Type getType() {
		return Type.ORDINAL;
	}
	
	public static Value makeValue(String val, int i) {
		Pair<String, Integer> input = new Pair<String,Integer>(val,i);
		if (!created.containsKey(input)) {
			Ordinal ord = new Ordinal(val, i);
			created.put(input, ord);
		}
		return created.get(input);		
	}
	
}