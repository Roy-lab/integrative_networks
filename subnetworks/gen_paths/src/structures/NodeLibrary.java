package structures;


/**
 * Holds onto information about nodes (which are otherwise represented as strings)
 * Allows retrieval and lets us add a feature at at time.
 * @author chasman
 */

//Things to keep track of for each node: (* indicates definitely need for operating)
// type (gene/protein/complex, possibly; we'll be starting with just genes.)
// * organism (host, virus)
// name (for easy reading)
// * experimental phenotype info: known hit, known interface, neither
// source(s) for hit/interface status
// * subcellular location(s) (set of strings)
// * other categories relevant to views


public class NodeLibrary extends Library<String> {

	/*
	 * Each implementation of Library should fill this in appropriately.
	 */
	protected static final String CONTENT_TYPE="Node";

	
	/**
	 * constructor
	 */
	public NodeLibrary() {
		super();
	}
	
	protected String getContentType() {
		return CONTENT_TYPE;
	}

	@Override
	protected void cleanup(String item) {
		assert(!this.contains(item)) : "Remove node from library before cleaning up";
		return;		
	}
	
	/**
	 * Summarize the features in the library.
	 * @return
	 */
//	public String toString() {
//		StringBuilder sb = new StringBuilder("Node library: ");
//		sb.append(String.format("%d feature(s) covering %d nodes", this.features.size(), this.featMap.size()));
//		for (Feature f : this.features) {
//			sb.append(String.format("\n\t%s\t%d nodes", f.toString(), this.get(f).size()));
//		}		
//		return sb.toString();
//	}
	
	/**
	 * Given a set of nodes, prints out the number of items of each feature.
	 * 
	 */
	
//	/**
//	 * Adds a feature and its node values to this library. 
//	 * Returns "true" if successful and "false" if this feature already exists.
//	 * @param feature
//	 * @param values
//	 * @return
//	 */
//	public boolean addValues(Feature feature, Map<String, Value> values) {
//		if (this.hasFeature(feature)) return false;
//		boolean canAdd = this.addFeature(feature);
//		
//		if (!canAdd) return false;
//				
//		HashMap<Value, HashSet<String>> subBack = this.backMap.get(feature);
//		
//		for (Entry<String, Value> entry : values.entrySet()) {
//			String node=entry.getKey();
//			Value val = entry.getValue();
//			if (!featMap.containsKey(node)) featMap.put(node, new HashMap<Feature, Value>());
//						
//			// I think this would only happen if the features list got out of sync 
//			assert(!hasFeature(node,feature)) : "Duplicate feature value."; 
//			featMap.get(node).put(feature, val);
//			
//			// for continuous values, value wouldn't necessarily already
//			// be in the subBack
//			if (!subBack.containsKey(val)) {
//				subBack.put(val, new HashSet<String>());
//			}
//			subBack.get(val).add(node);
//		}
//		return true;
//	}
//	
//	/**
//	 * Adds a feature to the necessary internal structures.
//	 * @param feature
//	 * @return
//	 */
//	protected boolean addFeature(Feature feature) {
//		boolean ok = this.features.add(feature);		
//		if (!ok) return false;
//		
//		this.featureNames.put(feature.name(), feature);
//		
//		this.backMap.put(feature, new HashMap<Value, HashSet<String>>());
//		Value[] vs = feature.values();
//		for (Value v : vs) {
//			this.backMap.get(feature).put(v, new HashSet<String>());
//		}
//		return true;
//	}
	
//	public Feature getFeature(String featName) {
//		return this.featureNames.get(featName);
//	}
//	
//	public boolean hasFeature(Feature feat) {
//		//return this.features.contains(feat);
//		return this.featureNames.containsValue(feat);
//	}
//
//	public boolean hasFeature(String node, Feature feat) {
//		return (featMap.containsKey(node) && featMap.get(node).containsKey(feat));
//	}
//	
//	public Set<Feature> features() {
//		return Collections.unmodifiableSet(this.features);
//	}
	
//	/**
//	 * Gets the features for a node.
//	 * @param e	edge
//	 * @return	unmodifiable feature map, or null if no edge present.
//	 */
//	public Map<Feature, Value> getFeatures(String n) {
//		if (this.featMap.containsKey(n)) {
//			return Collections.unmodifiableMap(this.featMap.get(n));
//		} else {
//			return null;
//		}
//	}
//
//	public Set<String> nodes() {
//		return Collections.unmodifiableSet(this.featMap.keySet());
//	}
	
//	/**
//	 * Get the value for a node for a feature
//	 * @param node
//	 * @param feat
//	 */
//	public Value getValue(String node, Feature feat) {
//		if (this.hasFeature(node, feat)) return featMap.get(node).get(feat);
//		else return null;
//	}
	
//	/**
//	 * Gets the nodes that have a particular feature,
//	 * or null if feature not here.
//	 * @return
//	 */
//	public Set<String> get(Feature f) {
//		if (!this.hasFeature(f)) return null;
//		HashMap<Value, HashSet<String>> nodes = this.backMap.get(f);
//		HashSet<String> got = new HashSet<String>();
//		for (HashSet<String> set : nodes.values()) {
//			got.addAll(set);
//		}
//		return got;
//	}
	
//	/**
//	 * Gets the nodes that have this value.
//	 * @param f
//	 * @param v
//	 * @return
//	 */
//	public Set<String> get(Feature f, Value v) {
//		if (!this.hasFeature(f)) return null;
//		HashMap<Value, HashSet<String>> nodes = this.backMap.get(f);
//		return Collections.unmodifiableSet(nodes.get(v));
//	}
//	


}
