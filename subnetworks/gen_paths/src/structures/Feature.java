package structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import structures.Value.Type;
import exceptions.InvalidValueException;

/**
 * Features have: a type, a value,
 * static: range/set of possible values, source citation
 * knowledge of how to printing themselves...
 * 
 * Features are immutable. 
 * 
 * @author chasman
 *
 */

public final class Feature {
	private final int SEED = 12345;
	private final Random rand = new Random(SEED);

	private final String name;
	private final Value.Type type; //discrete, continuous, ordinal, categorical set
	private final Value[] values; //possible values (or bounds of range)
	private final String note, source;
	private final String toString;	// feature is uniquely identifiable by string
	
	public static final String DELIM="\\|";
	
	private static final int MIN=0, MAX=1; // indices into the "values" array for sorted values 
	
	// default feature
	public static final Feature DEFAULT = 
		new Feature("DEFAULT", Value.Type.DISCRETE, 
				new Value[] {Discrete.makeValue("default")}, 
				"default","default");
	
	/**
	 * Construct a new feature.
	 * @param name
	 * @param type
	 * @param values
	 */
	public Feature(String name, Value.Type type, Value[] values, String note, String source) {
		this.name = name;		
		this.type = type;
		this.values = values;
		this.note = note;
		this.source = source;
		this.toString = this.makeToString();
	}
	
	/**
	 * Returns true if the values for this feature have a meaningful ordering.
	 * @return
	 */
	public boolean isComparable() {
		return this.type.isComparable();
	}
	
	public String name() {
		return name;
	}
	
	/**
	 * Returns the feature value type
	 * @return
	 */
	public Value.Type type() {
		return this.type;
	}
	
	public Value[] values() {
		return values;
	}
	
	public String note() {
		return this.note;
	}
	
	public String source() {
		return this.source;
	}
	
	/**
	 * Only applicable to continuous or otherwise-ordered features.
	 */
	public Value max() {
		assert(this.type == Value.Type.CONTINUOUS):"Can't call this on " +
				"a non-continuous feature.";		
		return values[MAX];		
	}
	/**
	 * Only applicable to continuous or otherwise-ordered features.
	 */
	public Value min() {
		assert(this.type == Value.Type.CONTINUOUS):"Can't call this on " +
				"a non-continuous feature.";		
		return values[MIN];		
	}
	
	/**
	 * Test for string in values. Used to test both discrete and set vals.
	 * @param testValue
	 * @return
	 */
	private boolean legalString(String testValue) {
		for (Value s : values) {
			if (s.toString().equals(testValue)) {
				return true;
			}
		}
		//not found?
		return false;
	}
	
	private boolean legalContinuous(String testValue) {
		//if we might be dealing with doubles, we should tack on a .0.
		double dMin = ((Continuous) values[0]).getValue();
		double dMax = ((Continuous) values[1]).getValue();

		double val = -1;
		try {
			val = Double.parseDouble(testValue);
		} catch (NumberFormatException e) {
			return false;
		}			
		
		if (val >= dMin && val <= dMax) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Tests legality of a value for this feature.
	 * If legal, returns the value.
	 * @param value	string format (even if value is numeric)
	 * @return value if legal, null otherwise
	 */
	public Value legal(String testValue) {			
		
		//case 1: discrete feature, singular or item in a set
		//value must be in my array, as a string.
		if (this.type == Value.Type.DISCRETE) {
			if(legalString(testValue)) {
				return Discrete.makeValue(testValue);
			} else {
				return null;
			}
		}
		//case 2: continuous feature.
		//value must be within the range defined by my array.
		else if (this.type == Value.Type.CONTINUOUS) {
			if (legalContinuous(testValue)) {
				return Continuous.makeValue(testValue);
			} else {
				return null;
			}
		}	
		// case three: set of discrete items, delimited by "|"
		else if (this.type == Value.Type.SET) {
			// split on delimiter and test
			String[] vals = testValue.split(DELIM);
			if (vals.length == 0) return null;
			HashSet<Discrete> set=new HashSet<Discrete>();
			for (String v : vals) {
				if (!legalString(v)) {
					return null;
				} else {
					set.add(Discrete.makeValue(v));
				}
			}
			return CatSet.makeValue(set);	// all were legal
		}
		
		// IDK what this would be - not implemented?
		assert(false) : String.format("Value type %s not implemented (Feature %s)", 
				this.type, this.name);
		return null;
	}
	
	@Override
	public String toString() {
		return this.toString;
	}
	
	private String makeToString() {
		String vals = "";
		for (int i = 0; i < this.values.length; i++) {
			vals += values[i] + ",";
		}
		//remove last comma
		String s = String.format("%s\t%s\t[%s]", this.name, this.type.toString(), vals.substring(0,vals.length()-1));	
		
		return s;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Feature) { 
			return this.toString().equals(other.toString());
		}
		else return false;
	}
	
	@Override
	public int hashCode() {
		return this.toString.hashCode();
	}
	
	/**
	 * 
	 * Reads a Feature file.
	 * I imagine that you're reading from a config file that defines a feature like this:
	 * name	valType	range/values	notes	filename	column
	 * 
	 * At this point, you've constructed the Feature using fields 1-4, and you're reading the file.
	 * 
	 * The file should be in this format:
	 * nodeID	value
	 * with *unique* node IDs. (We'll throw an exception if not unique.)
	 * Comments are #. 
	 *  
	 * Failure modes: can't find file, illegal values, node IDs aren't unique
	 * 
	 * If file exists but is empty, that's okay - we might have created the file programmatically and
	 * it's OK for the file to be empty. Make the feature but don't populate any values.
	 * 
	 * @param feature
	 * @param filename	shouldn't be null!
	 * @param delim	delimiter for file
	 * @param column in which feature is stored
	 * @return	the read feature values
	 * @throws IOException	if failure to find, open file 
	 * @throws InvalidValueException if bad format 
	 */
	public static HashMap<String, Value> readNodeFeature(Feature feature, String filename, String delim, int col) 
	throws IOException, InvalidValueException {
		Scanner s=null;
		try {
			// filename shouldn't be null
			s=new Scanner(new File(filename));
		} catch (FileNotFoundException fnfe) {
			throw new FileNotFoundException(
					String.format("Couldn't find the feature file %s", filename));
		}
		HashMap<String, Value> values = new HashMap<String, Value>();
		
		while (s.hasNext()) {
			String line = s.nextLine().trim();
			
			// skip comments
			if (line.startsWith("#")) {
				continue;
			}
			String[] sp = line.split(delim);
			
			// problem if too few fields? no - just means that the node isn't labelled
			// with this feature. skip.
			if (sp.length < col+1) {
				continue;
				//throw new InvalidValueException(
				//		String.format("Feature %s, file %s: Too few fields on line '%s'", 
				//				feature.name, filename, line));
			}
			
			String nodeID=Node.makeNode(sp[0].trim());
			if (values.containsKey(nodeID)) {
				throw new InvalidValueException(
						String.format("Feature %s, file %s: Duplicate node key '%s'", 
								feature.name, filename, nodeID));
			}
			
			// if sp[col] empty, skip.
			if (sp[col].length()==0) continue;
			
			Value value = feature.legal(sp[col]);
			if (value==null) {
				throw new InvalidValueException(
						String.format("Feature %s, file %s: Invalid value '%s'", 
								feature.name, filename, sp[col]));
			}
			
			//  store the value			
			values.put(nodeID, value);
		}	
		
		
		/*
		 * if (values.size() == 0) throw new InvalidValueException(
		 *
		 *		String.format("Feature %s, file %s: No values in file?", 
		 *				feature.name, filename));
		*/
		return values;
	}
	
	/**
	 * Reads a node set from a file and applies a default feature value.
	 * @param feature
	 * @param filename
	 * @param delim
	 * @param col	column of node ID
	 * @return
	 * @throws IOException
	 * @throws InvalidValueException
	 */
	public static HashMap<String, Value> readNodeFeatureDefaultValue(Feature feature, Value value, String filename, String delim, int col) 
	throws IOException, InvalidValueException {
		Scanner s=null;
		try {
			// filename shouldn't be null
			s=new Scanner(new File(filename));
		} catch (FileNotFoundException fnfe) {
			throw new FileNotFoundException(
					String.format("Couldn't find the feature file %s", filename));
		}
		HashMap<String, Value> values = new HashMap<String, Value>();
		
		while (s.hasNext()) {
			String line = s.nextLine().trim();
			
			// skip comments
			if (line.startsWith("#")) {
				continue;
			}
			String[] sp = line.split(delim);
			
			// problem if too few fields? yes.
			if (sp.length < col+1) {
				throw new InvalidValueException(
						String.format("Trying to read node set for Feature %s, file %s: Too few fields on line '%s'", 
								feature.name, filename, line));
			}
			
			String nodeID=Node.makeNode(sp[col]);
			if (nodeID.length() == 0) {
				throw new InvalidValueException(
						String.format("Feature %s, file %s: No node node key on this line '%s'", 
								feature.name, filename, line));
			}
			// throw error if duplicate assignment without same value
			else if (values.containsKey(nodeID) && !values.get(nodeID).equals(value)) {
				throw new InvalidValueException(
						String.format("Feature %s, file %s: Duplicate node key '%s'", 
								feature.name, filename, nodeID));
			} 			
			
			//  store the value			
			values.put(nodeID, value);
		}	
		
		
		if (values.size() == 0) throw new InvalidValueException(
				String.format("Feature %s, file %s: No nodes in file?", 
						feature.name, filename));
		return values;
	}
	
	
	
	
	/**
	 * Tries to read a feature from a string in this style:
	 * name=Type(valA|valB)
	 * @param dec
	 * @param note	can provide note optionally
	 * @param source	can provide the filename optionally
	 * @return
	 */
	public static Feature readFeatureDeclaration(String dec, 
			String note, String source) throws InvalidValueException  {
		String PAT_STR = "([0-9A-Za-z_\\-]+)=([\\.0-9A-Za-z_\\-]+)\\(([\\.0-9A-Za-z_\\-\\|]+)\\)";	
		Pattern PAT=Pattern.compile(PAT_STR);		
		Matcher m = PAT.matcher(dec);
		if (!m.find()) {
			throw new InvalidValueException("Can't build a feature out of this string: " + dec);
		}
		
		String name = m.group(1);
		Value.Type type = Value.Type.fromString(m.group(2).toUpperCase());
		String[] valStrs = m.group(3).split(DELIM);
		Value[] vals = Value.convert(type, valStrs);
		Feature feat = new Feature(name, type, vals, note, source);
		return feat;
	}
	
	public static Set<String> getFeatureNames(Set<Feature> feats) {
		HashSet<String> names = new HashSet<String>();
		for (Feature f : feats) {
			names.add(f.name());
		}
		return names;
	}
	
	/**
	 * Returns a random legal value.
	 * For continuous, we'll return a random double between min and max.
	 * For discrete and ordinal, we'll return a random single value.
	 * For CatSet, we'll pick a random subset of legal values.
	 * @return
	 */
	public Value random() {
		if (this.type == Type.CONTINUOUS) {
			double max = ((Continuous) this.max()).getValue();
			double min = ((Continuous) this.min()).getValue();
			double r = min + this.rand.nextDouble()*(max-min);
			return Continuous.makeValue((new Double(r)).toString());
		} 
		else if (this.type == Type.DISCRETE || this.type == Type.ORDINAL) {
			int r = rand.nextInt(this.values.length);
			return values[r];
		} 
		else if (this.type == Type.SET) {
			ArrayList<Discrete> vals = new ArrayList<Discrete>();
			
			// how many values to have in set? 
			// choose random integer from 1-len(values)
			double numVals = rand.nextInt(this.values.length-1) + 1.0;
			double prop = numVals / this.values.length;
			//System.out.format("Feature %s: pick %d random vals (%.3f%%) \n", this.name, (int) numVals, prop);
			
			for (Value v : this.values) {
				double test = rand.nextDouble();
				//System.out.format("\t%s: %f > %f ?\n", v, prop, test);
				if (test <= prop) vals.add((Discrete) v);
			}
			//System.out.println("[" + StringUtils.join(vals,"|") + "]");
		
			return CatSet.makeValue(vals);
		}
		// not implemented
		assert(false):"Not implemented";
		return null;
	}
	
	/**
	 * Returns some number of randomly chosen legal values.
	 * Might have duplicates. This is really just for
	 * testing, so that's cool. 
	 * 
	 * @param num
	 * @return
	 */
	public Value[] random(int num) {
		
		Value[] vals = new Value[num];
		for (int i=0; i < vals.length; i++) {
			if (this.type == Type.CONTINUOUS) {
				double max = ((Continuous) this.max()).getValue();
				double min = ((Continuous) this.min()).getValue();
				double r = min + this.rand.nextDouble()*(max-min);
				vals[i]= Continuous.makeValue((new Double(r)).toString());
			} 
			else {
				int r = rand.nextInt(this.values.length);
				vals[i]= values[r];
			} 
		}
		return vals;
	}
	
		
}
