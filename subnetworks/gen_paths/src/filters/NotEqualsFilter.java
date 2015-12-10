package filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import structures.CatSet;
import structures.Discrete;
import structures.Feature;
import structures.Value;
import structures.Value.Type;
import utilities.StringUtils;
import exceptions.InvalidValueException;

/**
 * Used to filter nodes by discrete feature values
 * or presence of some feature value in a categorical set.
 * 
 * Returns opposite of EqualsFilter.
 * 
 * For discrete, ordinal, or continuous features, REJECTS nodes for which the 
 * value is in the legal value set.
 * For set features, REJECTS nodes that have an overlap (any) with the 
 * legal value set. 
 * 
 * @author chasman
 *
 */

public class NotEqualsFilter extends EqualsFilter {

	protected final HashSet<Value> legal;

	public static final String VAL_DELIM="\\|";


	private NotEqualsFilter(String name, Feature f, boolean acceptNull, Value[] legal) {
		super(name, f, acceptNull, legal);
		this.legal= new HashSet<Value>(Arrays.asList(legal));
	}

	/**
	 * Returns inverse of EqualsFilter.
	 * @param v
	 * @return
	 */
	public boolean accept(Value v) {
		return !(super.accept(v));	
	}

	public String toString() {
		String acceptNull = this.acceptNull ? "|null" : "";
		return String.format("NotEquals %s [%s%s]", feature.name(), 
				StringUtils.join(this.legal, "|"), acceptNull);
	}

	/**
	 * Make a NotEquals node filter. 
	 * Legal values are given separated by pipes - so, catset values are
	 * going to be interpreted as a set of discrete values.
	 * 
	 * Line looks like this:
	 * NFILTER	name	type	feature	accepted_vals
	 * NFILTER	viral	EqualsFilter	hiv_genes	virus
	 * 
	 * If "null" in the list of accepted vals, then we'll accept 
	 * items that haven't been assigned a value for this feature. 
	 * 
	 * @param line
	 * @param feature
	 * @return
	 * @throws InvalidValueException
	 */
	public static NotEqualsFilter makeFilter(String[] line, Feature f) 
	throws InvalidValueException {
		String name=line[1];
		//Feature f = libe.getFeature(line[3]);
		if (f==null) {
			throw new InvalidValueException(
					String.format("Undefined feature %s expected by NotEqualsFilter %s.", 
							line[3], name));
		}
		
		String valString = line[4];
		boolean acceptNull = (line[4].contains("null"));
		if (acceptNull) {
			valString = line[4].replace("|null", "").replace("null|", "").replace("null", "");
		}
		

		// one special case - if filter feature is catset, then 
		// read values as Discrete.
		// another special case: we can specify 'null' as a valid value.
		String[] vals = valString.split(VAL_DELIM);		
		Value[] v = new Value[vals.length];
		
		if (f.type() == Type.SET) {
			CatSet vi = (CatSet) f.legal(valString);
			
			if (vi==null) {
				throw new InvalidValueException(
						String.format("%s is not a legal value for Feature %s.",
								vi, f.toString()));
			}
			
			Set<Discrete> catvals = vi.getValue();
			Iterator<Discrete> catiter = catvals.iterator();
			for (int i=0; i<vals.length; i++) {
				v[i] = catiter.next();				
			}

		} else {	
			for (int i=0; i<vals.length; i++) {
				v[i] = f.legal(vals[i]);	
				if (v[i]==null) {
					throw new InvalidValueException(
							String.format("%s is not a legal value for Feature %s.",
									vals[i], f.toString()));
				}
			}		
		}

		// the other maker-method will test for legality
		return NotEqualsFilter.makeFilter(name, f, acceptNull, v);
	}

	/**
	 * @param f	the feature in question
	 * @param threshold	a threshold value for the acceptance decision
	 * @param order	the order that an accepted node must have with the threshold
	 */
	public static NotEqualsFilter makeFilter(String name, Feature f, boolean acceptNull, Value[] legal) 
	throws InvalidValueException {
		// check all values against feature
		for (Value v : legal) {	
			if (v==null) {
				System.err.println("err");
			}
	
			if (f.legal(v.toString())==null) {
				throw new InvalidValueException(
						String.format("In this filter, " +
								"cannot combine feature %s and value %s.", 
								f.name(), v.toString()));
			}
		}				
		return new NotEqualsFilter(name, f, acceptNull, legal);
	}

}
