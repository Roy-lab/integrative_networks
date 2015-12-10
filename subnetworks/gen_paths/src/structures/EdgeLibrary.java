package structures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import utilities.Enums.Sign;
import exceptions.DuplicateException;
import exceptions.InvalidValueException;

/**
 * Stores information about edges.
 * Now, we allow multiple interactions between same pair: up to four (one of each
 * sign/direction configuration)
 * Also provides tools for reading edges, since all of the attached stuff in my
 * tab files will get put into the library.
 * @author chasman
 *
 */

public class EdgeLibrary extends Library<Edge>{
	

	/*
	 * Each implementation of Library should fill this in appropriately.
	 */
	protected static final String CONTENT_TYPE="Edge";

	

	public static final String COMMENT="#";

	/*
	 * Holds onto the filenames that went into this library.
	 */
	protected ArrayList<String> filenames;
	

	/*
	 * Holds onto the source filename(s) for each edge
	 */
	protected HashMap<Edge, ArrayList<String>> edgeFns;
	
	/*
	 * Holds onto names of unbound features
	 */
	protected HashSet<String> unboundFeatNames;
	
	/*
	 * Features with unbound values - use strings for all.
	 * Use this for things like lists of PMIDs and other source information.
	 */
	protected HashMap<Edge, HashMap<String,String>> unboundFeats;

	/*
	 * When we read edges, we'll store the number of times each
	 * edge (a.b.d.s) appears in the original data.
	 * The "collapse" function will update the values.
	 */
	public static final Feature COUNT_FEATURE = 
		new Feature("count", 
				Value.Type.CONTINUOUS, 
				new Value[] { Continuous.makeValue(0), 
				Continuous.makeValue(Integer.MAX_VALUE)},
				"count of instances of this edge", 
		"on-demand");

	/**
	 * constructor
	 */
	public EdgeLibrary() {
		super();
		filenames = new ArrayList<String>();
		edgeFns = new HashMap<Edge, ArrayList<String>>();
		unboundFeats = new HashMap<Edge, HashMap<String,String>>();
		if (!this.hasFeature(COUNT_FEATURE)) this.addFeature(COUNT_FEATURE);

		//featMap=new HashMap<Edge, HashMap<Feature, Value>>();
		//backMap = new HashMap<Feature, HashMap<Value, HashSet<Edge>>>();
		//features=new HashSet<Feature>();
	}
	
	
	/**
	 * Get the type of thing that is stored in this library.
	 * @return
	 */
	protected String getContentType() {
		return CONTENT_TYPE;
	}


	public List<String> getFilenames() {
		return Collections.unmodifiableList(this.filenames);
	}

	/**
	 * Gets the filenames in support of a query edge.
	 * @param e
	 * @return
	 */
	public List<String> getFilenames(Edge e) {
		return Collections.unmodifiableList(this.edgeFns.get(e));
	}

	public Set<String> getUnboundFeatureNames() {
		return Collections.unmodifiableSet(this.unboundFeatNames);
	}
	public Map<String,String> getUnboundFeatures(Edge e) {	
		if (!this.unboundFeats.containsKey(e)) return null;
		return Collections.unmodifiableMap(this.unboundFeats.get(e));
	}
	public boolean hasUnboundFeatures(Edge e) {
		return this.unboundFeats.containsKey(e);
	}
	
	/**
	 * Returns true if the library contains an edge of any type
	 * between a and b.
	 * @param a
	 * @param b
	 * @return
	 */
	public HashSet<Edge> containsConnection(String a, String b) {
		// check all possibilities		
		HashSet<Edge> contained=new HashSet<Edge>();

		for (boolean dir : new boolean[] { true, false }) {
			for (Sign sign : Sign.values()) {
				Edge test1 = new Edge(a, b, dir, sign);
				Edge test2 = new Edge(b, a, dir, sign);
				if (this.contains(test1)) contained.add(test1);
				if (this.contains(test2)) contained.add(test2);
			}
		}

		return contained;
	}

	/**
	 * Returns the edge if any edge between this nodes is in the library,
	 * or null if none.
	 * @param e
	 * @return
	 */
	public HashSet<Edge> containsConnection(Edge e) {
		return containsConnection(e.i, e.j);
	}

	/**
	 * Returns true if this edge is in the library AND is the only connection
	 * between these two nodes.
	 * @param e
	 * @return
	 */
	protected boolean uniqueConnection(Edge e) {
		HashSet<Edge> contained = this.containsConnection(e);
		return (contained.contains(e) && contained.size() == 1);
	}
	
	/**
	 * Removes filename for edge. Assume we have already removed the edge
	 * from the library.
	 */
	@Override
	protected void cleanup(Edge e) {
		assert(!this.contains(e)) : "Remove edge from library before cleanup";
		this.edgeFns.remove(e);
		this.unboundFeats.remove(e);
	}

//	/**
//	 * Tries to add the features for an edge.
//	 * 
//	 * Returns false if we try to add a different value for a 
//	 * previously-defined feature.
//	 * @param e
//	 * @param vals
//	 * @return
//	 */
//	public boolean addValues(Edge e, Map<Feature, Value> vals) {
//		// if edge present, check for overlap in feature names/values.
//		boolean ok = true;
//		if (this.contains(e)) {
//
//			Map<Feature, Value> myFeats=this.getFeatures(e);
//			ArrayList<Feature> newFeats = new ArrayList<Feature>(vals.keySet());
//			for (Feature f : newFeats) {
//				if (myFeats.containsKey(f)) {
//
//					// did we already declare it and not a set-valued feature?
//					if (vals.get(f) != myFeats.get(f) && !f.type().equals(Value.Type.SET)) {
//						ok=false;
//					}
//					// if cat set, then merge values	
//					else if (vals.get(f) != myFeats.get(f) && f.type().equals(Value.Type.SET)) {
//						CatSet newVal = (CatSet) vals.get(f);
//						CatSet curVal = (CatSet) myFeats.get(f);
//						HashSet<Discrete> newSet = new HashSet<Discrete>(curVal.getValue());						
//						newSet.addAll(newVal.getValue());
//
//						vals.put(f, CatSet.makeValue(newSet));
//					}
//				}
//			}
//		}
//
//		// compatible values?
//		if (ok) {
//			// okay! let's proceed.		
//			for (Entry<Feature, Value> entry : vals.entrySet()) {
//				// add feature if not already present
//				boolean addFeature = this.addFeature(entry.getKey());
//
//				// force OK because we checked already
//				boolean ok2 = addValue(e, entry.getKey(), entry.getValue(), true);
//				assert(ok2) : 
//					String.format("Problem adding feature %s for edge %s", entry.getKey().name(), e);				
//			}
//		}
//
//
//		return ok;		
//	}
//
//	/**
//	 * Adds this feature value for this edge.
//	 * Private because we want to make sure in advance that
//	 * we're not duplicating information or overwriting values willy-nilly.
//	 * Assume feature was already added properly.
//	 * 
//	 * @param e
//	 * @param f
//	 * @param val
//	 * @param force	to force-overwrite an existing value
//	 * @return	false if exact feature already exists and value isn't same
//	 */
//	private boolean addValue(Edge e, Feature f, Value val, boolean force) {
//		assert(this.features.contains(f)) : 
//			String.format("Feature %s hasn't been added already.", f.name());
//
//		// return false if feature exists with different value (and not forcing)
//		// can return true if feature exists with same value; no need to add.
//		boolean overwriting=false;
//		if (this.hasFeature(e, f)) {
//			if (val != this.getValue(e, f) && !force) {
//				return false;
//			}
//			if (val == this.getValue(e, f)) {
//				return true;
//			} 
//			if (force) {
//				overwriting=true;
//			}
//		}
//
//		if (!this.contains(e)) {
//			this.featMap.put(e, new HashMap<Feature, Value>());
//		}
//		// overwrites!
//		Value add = this.featMap.get(e).put(f, val);
//
//		if (add != null && !force) return false;		
//
//		// now do in reverse
//		if (!(val instanceof CatSet)) {
//			HashMap<Value, HashSet<Edge>> subBack = this.backMap.get(f);
//			assert( subBack!=null && val!=null) :
//				"wtf";
//
//			if (!subBack.containsKey(val)) {
//				subBack.put(val, new HashSet<Edge>());
//			}
//			subBack.get(val).add(e);
//
//			// if overwriting, remove old value ("add")
//			if (overwriting) {
//				subBack.get(add).remove(e);
//			}
//		}
//		else {
//			CatSet catval = (CatSet) val;
//			for (Value v : catval.getValue()) {
//				HashMap<Value, HashSet<Edge>> subBack = this.backMap.get(f);
//				assert( subBack!=null && val!=null) :
//					"wtf";
//
//				if (!subBack.containsKey(v)) {
//					subBack.put(val, new HashSet<Edge>());
//				}
//				subBack.get(v).add(e);
//
//				// we don't overwrite values with catsets!
//			}
//		}
//
//
//
//		return true;
//	}	


	/**
	 * Merges another edge library with this one.
	 * Throws exception if features for same edge are redefined.
	 * @param other
	 */
	public boolean addAll(EdgeLibrary other) throws IncompatibleException {

		for (Edge e : other.items()) {			
			// get all features except count			
			HashMap<Feature, Value> feats = new HashMap<Feature, Value>(other.getFeatures(e));
			feats.remove(COUNT_FEATURE);

			double ocount = ((Continuous) other.getValue(e, COUNT_FEATURE)).getValue();
			double thiscount = this.contains(e) ?
					((Continuous) this.getValue(e, COUNT_FEATURE)).getValue() :
						0.0;			
					boolean ok = this.addValues(e, feats);

					if (!ok) {				
						throw new IncompatibleException(
						"Incoming EdgeLibrary overwrites one or more features for some edge.");
					}

					// update count

					this.addValue(e, COUNT_FEATURE, Continuous.makeValue(ocount+thiscount), true);

					// combine filenames
					if (!this.edgeFns.containsKey(e)) this.edgeFns.put(e, new ArrayList<String>());
					this.edgeFns.get(e).addAll(other.edgeFns.get(e));
		}		
		
		if (this.unboundFeatNames != null) {
			this.unboundFeatNames.addAll(other.unboundFeatNames);
		} else {
			this.unboundFeatNames = other.unboundFeatNames;
		}
		if (this.unboundFeats != null) {
			this.unboundFeats.putAll(other.unboundFeats);
		} else { 
			this.unboundFeats = other.unboundFeats;
		}
		return this.filenames.addAll(other.filenames);
	}
	

	/**
	 * Replaces one node with another in this edge library.
	 * Usage example: say we want to merge DOT6 and TOD6 into a new node, DOT6TOD6.
	 * This entails copying over all edges (and feature information) for DOT6 and TOD6
	 * to the new node. 
	 * Removes the original after copying.
	 *  
	 * @param orig
	 * @param replacement
	 * @return
	 */
	public HashSet<Edge> replaceNode(String orig, String replacement) {
		// get all edges involving the original
		// time consuming
		String original=Node.makeNode(orig);
		HashSet<Edge> items = new HashSet<Edge>(this.items());	
		HashSet<Edge> removed = new HashSet<Edge>();
		for (Edge e : items) {
			if (e.nodes().contains(original)) {
				removed.add(e);
				Edge replacer = e.replaceNode(original, replacement);
				this.copyFeatures(e, replacer);
				this.edgeFns.put(replacer, this.edgeFns.get(e));
				this.unboundFeats.put(replacer, new HashMap<String,String>(this.getUnboundFeatures(e)));
			}			
		}
		for (Edge e : removed) {
			if (this.contains(e)) this.remove(e);
		}
		return removed;
	}
	
//
//	/**
//	 * prints out a string representation for reading
//	 */
//	public String toString() {
//		StringBuilder sb = new StringBuilder("Edge library: ");
//		sb.append(String.format("%d feature(s) covering %d edges", this.features.size(), this.featMap.size()));
//		for (Feature f : this.features) {
//			sb.append(String.format("\n\t%s\t%d edges", f.toString(), this.get(f).size()));
//		}
//
//		return sb.toString();
//	}

	/**
	 * Reads edges from a file. 
	 * Edge format:
	 * eid	node0	node1	direction	[other features]
	 * 
	 * We'll skip eid and make our own.
	 * The first line of the file will give us the names of edge features.
	 * We'll also automatically add "autoFeature" to each edge 0 - this is,
	 * for example, a feature describing the biological context of the edge 
	 * (host interaction or host-virus)
	 * 
	 * Counts up edge duplicates. Throws exception if feature values don't match.
	 * 
	 * @param filename	filename of edge file
	 * @param	start column
	 * @param autoFeats	features/values to apply to all edges
	 * @param readCols	read the features in these columns
	 * @param unboundFeatCols	read more features in these columns - we will not bound the values (source info, PMIDs, etc)
	 * @param srcFormat	optional format string to apply to source of edge
	 * @param tarFormat optional format string to apply to target of edge
	 * @return
	 */
	public static EdgeLibrary readEdges(
			String filename, int start, Map<Feature,Value> autoFeats, 
			HashSet<Integer> readCols, HashSet<Integer> unboundFeatCols, String srcFormat, String tarFormat) 
	throws IOException, InvalidValueException, DuplicateException {

		// make a new EdgeLibrary with the features
		EdgeLibrary el = new EdgeLibrary();

		Scanner s = null;
		try {
			s = new Scanner(new File(filename));
		

		//OK for autofeature to be null... IF we're reading columns.
		/*if (autoFeature==null && (readCols==null || readCols.size()==0)) {
			autoFeature = Feature.DEFAULT;
			autoValue = autoFeature.values()[0];
		}*/

		// add auto features
		if (autoFeats != null) {
			for (Feature f : autoFeats.keySet()) {
				boolean ok =  el.addFeature(f);
				assert(ok) : "Something wrong with addFeature on adding default feature " + f.name();
			}
		}


		// add the count feature
		el.addFeature(EdgeLibrary.COUNT_FEATURE);

		String[] headers=s.nextLine().split("\t");
		
		// if readCols provided, start building features.
		HashMap<Integer, Feature> readFeatures = new HashMap<Integer, Feature>();
		if (readCols != null) {
			
			for (Integer i : readCols) {
				String decl = headers[i];
				Feature feat = Feature.readFeatureDeclaration(decl, 
						String.format("column %d", i), filename);
				el.addFeature(feat);
				readFeatures.put(i, feat);
			}
			//System.out.println(Arrays.toString(headers));
		}		
		
		// same for unbound features
		HashMap<Edge, HashMap<String,String>> unboundFeats=new HashMap<Edge, HashMap<String,String>>();
		HashSet<String> unboundFeatNames=new HashSet<String>();
		if (unboundFeatCols != null) {
			for (Integer i : unboundFeatCols) {
				String name=headers[i];
				unboundFeatNames.add(name);				
			}
		}

		// keep track of counts
		HashMap<Edge, Integer> counts=new HashMap<Edge, Integer>();

		// read in the edges - allow first line to be nonstandard
		// in case it has some random info in it
		boolean first=true;
		
		while (s.hasNext()) {
			// ONLY trim off endline
			String line = s.nextLine();
			if (line.length() == 0) continue;
			if (line.startsWith(COMMENT) || line.length() == 0) continue;
			// title line
			if (line.contains("dir") && line.contains("sign")) {
				continue;
			}

			String[] split = line.split("\t");		
			// allow first line to be invalid
			if (first && split.length < 4) {
				first=false;
				continue;
			}
			
			// trim endline from last one
			split[split.length-1] = split[split.length-1].trim();
			
			if (split.length < 2) System.err.println(Arrays.toString(split));
			
			// apply source/target formatter if requested
			String aStr = srcFormat==null ? split[start] : String.format(srcFormat, split[start]); 
			String bStr = tarFormat==null ? split[start+1] : String.format(tarFormat, split[start+1]);
			
			String a = Node.makeNode(aStr);
			String b = Node.makeNode(bStr);			

			assert(!a.contains("#") && (!b.contains("#"))) : filename + " might be weird: " + line;

			boolean dir = split[start+2].trim().equals("1");

			// if undirected, make sure a & b are in alphabetical order
			if (!dir) {
				if (b.compareTo(a) < 0) {
					String temp=b;
					b=a;
					a=temp;
				}
			}

			Sign sign = Sign.fromValue(Integer.parseInt(split[start+3].trim()));

			Edge e = new Edge(a,b,dir,sign);

			// update counts	
			boolean dup=false;
			if (!el.contains(e)) {
				counts.put(e, 1);
			} else {
				counts.put(e, counts.get(e)+1);
				dup=true;
			}

			HashMap<Feature, Value> feats = new HashMap<Feature, Value>();

			// add auto values
			if (autoFeats != null) {
				for (Feature f : autoFeats.keySet()) {
					Value v = autoFeats.get(f);
					feats.put(f, v);	
				}				
			}

			// do other feature stuff here
			if (readCols != null) {
				for (Entry<Integer, Feature> entry : readFeatures.entrySet()) {
					Integer col = entry.getKey();
					Feature feat = entry.getValue();
					boolean hasVal=false;
					Value val=null;
					if (split.length > col && split[col].length() > 0) {
						val=feat.legal(split[col]);
						hasVal=(val != null);
					}
					// read illegal
					if (val==null && hasVal) { 
						throw new InvalidValueException(
								String.format("Bad value for feature %s: %s", feat.name(), split[col]));
					}
					// read OK value
					if (val != null) {
						feats.put(feat, val);
					}
				}				
			}
			
			if (unboundFeatCols != null) {
				for (Integer i : unboundFeatCols) {
					// may not have value if last column
					if (i>=split.length) 
						continue;
					
					String name = headers[i];					
					String val = split[i];
					if (!unboundFeats.containsKey(e)) {
						unboundFeats.put(e, new HashMap<String,String>());						
					}
					unboundFeats.get(e).put(name, val);
					
				}
			}

			// add the edge
			boolean ok = el.addValues(e, feats);

			// ok would be false if we tried to add a different value
			// for an existing edge
			if (!ok) {
				throw new DuplicateException(
						String.format("EdgeLibe %s: Some feature defined (at least) twice for edge %s",
								filename, e));
			}
		}

		// add the count feature values
		for (Edge e : counts.keySet()) {
			Value c = Continuous.makeValue(counts.get(e));
			boolean success = el.addValue(e, COUNT_FEATURE, c, false);
			assert(success) : 
				"Weird trouble adding count values when reading file? " + filename + ", " + e;
		}
		
		// add in the supplementary features
		if (unboundFeats != null) {
			el.unboundFeatNames=unboundFeatNames;
			el.unboundFeats=unboundFeats;
		}

		// add in filenames
		el.filenames.add(filename);
		for (Edge e : el.items()) {
			el.edgeFns.put(e, new ArrayList<String>());
			el.edgeFns.get(e).add(filename);
		}
		} catch (IOException ioe) {
			throw new IOException(String.format("Couldn't read edge file '%s'", filename));
		} finally {
			s.close();
		}

		return el;
	}
	

	
	/**
	 * WARNING: I don't entirely remember what I meant to do with this.
	 * Applies the EdgeCollapser to the library.
	 * This may result in some edges being merged together.
	 * Returns a copy of this library in which some edges may have
	 * been removed and others may have been redirected.
	 * @param collapser
	 * @return
	 */
	public EdgeLibrary collapse(EdgeCollapser collapser) {
		EdgeLibrary origLibe = this;
		// If redirection requested, do that first.
		if (collapser.doRedirecting()) {
			HashMap<Edge, HashSet<Edge>> map = collapser.redirect(this);
			origLibe = EdgeLibrary.resolve(this, map);
		}

		// Now try collapsing.
		HashMap<Edge, HashSet<Edge>> map = collapser.collapse(origLibe);
		EdgeLibrary resolved = EdgeLibrary.resolve(origLibe, map);
		return resolved;
	}

	/**
	 * Applies a map to an EdgeLibrary to produce a resolved library.
	 *
	 * @param orig	the original library
	 * @param map	for example, generated by an EdgeCollapser
	 * @return
	 */
	public static EdgeLibrary resolve(EdgeLibrary orig, HashMap<Edge, HashSet<Edge>> map) {
		// now the tough work of merging feature info
		EdgeLibrary newLibe = new EdgeLibrary();

		// add all features
		for (Feature f : orig.features) {
			newLibe.addFeature(f);
		}


		// original edge
		for (Edge oe : map.keySet()) {

			// get all features but the count
			HashMap<Feature, Value> ofeat = new HashMap<Feature,Value>(orig.getFeatures(oe));
			ofeat.remove(COUNT_FEATURE);

			// get filenames			
			HashSet<String> filenames = new HashSet<String>(orig.getFilenames(oe));

			// get OE's count
			Continuous oriCount = (Continuous) orig.getValue(oe, COUNT_FEATURE);

			// map to...
			for (Edge me : map.get(oe)) {

				// current count for mapped edge
				Continuous meCount = Continuous.makeValue(0);	
				// if ME not already in new libe, add all of its features
				// except for the count.				
				// possible for ME to NOT be in the original library.
				if (!newLibe.contains(me) && orig.contains(me)) {
					HashMap<Feature, Value> mefeats = new HashMap<Feature, Value>(orig.getFeatures(me));
					mefeats.remove(COUNT_FEATURE);

					boolean success = newLibe.addValues(me, mefeats);
					assert(success) : "EdgeLibrary.collapse: Problem adding original feature values for edge " + me;

					meCount = (Continuous) orig.getValue(me, COUNT_FEATURE);
				} else if (orig.contains(me)) {
					meCount = (Continuous) orig.getValue(me, COUNT_FEATURE);
				}

				// now try to add OE's features (except the count) to ME
				boolean success1 = newLibe.addValues(me, ofeat);
				assert(success1) : "You need to debug this, yo";

				// NOW, unless they are THE SAME EDGE, add the counts together and force-update.
				// if they ARE the same edge, or ME is new, then use the merged edge's count.				
				if (me.equals(oe)) {				
					newLibe.addValue(me, COUNT_FEATURE, meCount, true);
				} else {
					Continuous newCount = Continuous.makeValue(oriCount.getValue() + meCount.getValue());
					newLibe.addValue(me, COUNT_FEATURE, newCount, true);	
				}


				// merge filenames
				if (orig.contains(me)) {
					filenames.addAll(orig.getFilenames(me));
				}
				newLibe.edgeFns.put(me, new ArrayList<String>(filenames));
			}			
		}	

		// combine master filename list
		HashSet<String> masterfn = new HashSet<String>(orig.filenames);
		newLibe.filenames = new ArrayList<String>(masterfn);

		return newLibe;
	}

	//	/**
	//	 * Verifies that this edgelibrary doesn't contain duplicate
	//	 * edges between two nodes.
	//	 * @return	true if no duplicates.
	//	 */
	//	public boolean verifyNoDuplicates() {
	//		for (Edge e : this.featMap.keySet()) {
	//			boolean uniq = this.uniqueConnection(e);
	//			if (!uniq) 
	//				return false;
	//		}
	//		return true;
	//	}

	@SuppressWarnings("serial")
	public class IncompatibleException extends Exception {
		IncompatibleException(String message) {
			super(message);
		}
	}

}
