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
 * For discrete, ordinal, or continuous features, accepts nodes for which the 
 * value is in the legal value set.
 * For set features, accepts nodes that have an overlap (any) with the 
 * legal value set. 
 * 
 * @author chasman
 *
 */

public class EqualsFilter extends Filter {

	protected final HashSet<Value> legal;

	public static final String VAL_DELIM="\\|";


	protected EqualsFilter(String name, Feature f, boolean acceptNull, Value[] legal) {
		super(name, f, acceptNull);
		this.legal= new HashSet<Value>(Arrays.asList(legal));
	}

	public Feature feature() {
		return this.feature;
	}

	/**
	 * Returns the set of legal values accepted by this filter.
	 * @return
	 */
	public Set<Value> legal() {
		return Collections.unmodifiableSet(legal);
	}

	public boolean accept(Value v) {
		if (this.acceptNull && v==null) {
			return true;
		} else if (v==null) return false;

		// set values: check for intersection with legal values
		if (v.getType() == Type.SET) {
			CatSet catv = (CatSet) v;
			Set<Discrete> set = new HashSet<Discrete>(catv.getValue());
			set.retainAll(this.legal);
			return (set.size() > 0);
		} 
		// check if node's value in legal
		else {
			return (this.legal.contains(v));
		}	
	}

//	/**
//	 * For singularly-valued features, accept if value in legal set.
//	 * For set-valued features, accept if any overlap with legal set.
//	 */
//	public boolean accept(String node, NodeLibrary libe) {
//		Value v = libe.getValue(node, this.feature);
//		return accept(v);
//	}

	public String toString() {
		String acceptNull = this.acceptNull ? "|null" : "";
		return String.format("%s [%s%s]", feature.name(), 
				StringUtils.join(this.legal, "|"), acceptNull);
	}

	/**
	 * Make an equals node filter. 
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
	public static EqualsFilter makeFilter(String[] line, Feature f) 
	throws InvalidValueException {
		String name=line[1];
		//Feature f = libe.getFeature(line[3]);
		if (f==null) {
			throw new InvalidValueException(
					String.format("Undefined feature %s expected by EqualsFilter %s.", 
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
		return EqualsFilter.makeFilter(name, f, acceptNull, v);
	}

	/**
	 * @param f	the feature in question
	 * @param threshold	a threshold value for the acceptance decision
	 * @param order	the order that an accepted node must have with the threshold
	 */
	public static EqualsFilter makeFilter(String name, Feature f, boolean acceptNull, Value[] legal) 
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
		return new EqualsFilter(name, f, acceptNull, legal);
	}

}
