package structures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class Library<T> {	
	
	/*
	 * Each implementation should fill this in appropriately.
	 */
	protected static final String CONTENT_TYPE="Abstract";

	/*
	 * We can look up the values stored for each item.
	 */
	protected HashMap<T, HashMap<Feature, Value>> featMap;

	/*
	 * Or the items for each feature value.
	 */
	protected HashMap<Feature, HashMap<Value, HashSet<T>>> backMap;

	/*
	 * The features we have in here.
	 */
	protected HashSet<Feature> features;

	/*
	 * Retrieve features by name.
	 */
	protected HashMap<String, Feature> featureNames;	

	protected Library() {
		this.featMap=new HashMap<T, HashMap<Feature, Value>>();
		this.backMap=new HashMap<Feature, HashMap<Value, HashSet<T>>>();
		this.features=new HashSet<Feature>();
		this.featureNames=new HashMap<String, Feature>();
	}

	/**
	 * Adds a feature and its item values to this library. 
	 * Returns "true" if successful and "false" if this feature already exists.
	 * 
	 * 
	 * @param feature
	 * @param values
	 * @return
	 */
	public boolean addValues(Feature feature, Map<T, Value> values) {
		if (this.hasFeature(feature)) return false;
		boolean canAdd = this.addFeature(feature);

		if (!canAdd) return false;

		HashMap<Value, HashSet<T>> subBack = this.backMap.get(feature);

		for (Entry<T, Value> entry : values.entrySet()) {
			T item=entry.getKey();
			Value val = entry.getValue();
			if (!featMap.containsKey(item)) featMap.put(item, new HashMap<Feature, Value>());

			// I think this would only happen if the features list got out of sync 
			assert(!hasFeature(item,feature)) : "Duplicate feature value."; 
			featMap.get(item).put(feature, val);

			// for set-valued features, we want to be able to use either the
			// combination of or any of the individual values to retrieve the items
			if (val instanceof CatSet) {
				CatSet cats = (CatSet) val;
				for (Value v : cats.getValue()) {
					if (!subBack.containsKey(v)) {
						subBack.put(v, new HashSet<T>());
					}
					subBack.get(v).add(item);
				}
			} else {
				// for continuous values, value wouldn't necessarily already
				// be in the subBack.
				if (!subBack.containsKey(val)) {
					subBack.put(val, new HashSet<T>());
				}
				subBack.get(val).add(item);
			}
		}
		return true;
	}
	
	
	
	/**
	 * Get the type of thing that is stored in this library.
	 * @return
	 */
	protected abstract String getContentType();

	/**
	 * Summarize the features in the library.
	 * @return
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder(String.format("%s Library: ", this.getContentType()));
		sb.append(String.format("%d feature(s) covering %d %s(s)", this.features.size(), this.featMap.size(), this.getContentType().toLowerCase()));
		for (Feature f : this.features) {
			sb.append(String.format("\n\t%s\t%d %s(s)", f.toString(), this.get(f).size(), this.getContentType().toLowerCase()));
		}		
		return sb.toString();
	}

	
	/**
	 * Summarize the features for a given set of items.
	 * @param items
	 * @return
	 */
	public String summarize(Set<T> items) {
		StringBuilder sb = new StringBuilder(String.format("Summarizing features from %d %ss:", items.size(), this.getContentType().toLowerCase()));

		for (Feature f : this.features) {
			HashSet<T> intersect = new HashSet<T>(this.get(f));
			int orig=intersect.size();
			intersect.retainAll(items);
			sb.append(String.format("\n\t%s\t%d / %d %s(s)", f.toString(), intersect.size(), orig, this.getContentType().toLowerCase()));
		}
		
		return sb.toString();
	}

	/*
	 * Number of items in the library.
	 * @return
	 */
	public int size() {
		return this.featMap.size();
	}
	/**
	 * Gets a feature by name.
	 * @param name
	 * @return
	 */
	public Feature getFeature(String name) {
		return this.featureNames.get(name);
	}

	/**
	 * Gets the set of all features in the library (unmodifiable).
	 * @return
	 */
	public Set<Feature> getFeatures() {
		return Collections.unmodifiableSet(this.backMap.keySet());
	}

	/**
	 * Gets the features for an edge.
	 * @param e	edge
	 * @return	unmodifiable feature map, or null if no edge present.
	 */
	/**
	 * Gets the features for a item.
	 * @param e	edge
	 * @return	unmodifiable feature map, or null if no edge present.
	 */
	public Map<Feature, Value> getFeatures(T n) {
		if (this.featMap.containsKey(n)) {
			return Collections.unmodifiableMap(this.featMap.get(n));
		} else {
			return null;
		}
	}

	public Set<T> items() {
		return Collections.unmodifiableSet(this.featMap.keySet());
	}

	public boolean contains(T item) {
		return this.featMap.containsKey(item);
	}

	public boolean hasFeature(Feature feat) {
		return this.features.contains(feat);
	}

	public boolean hasFeature(T item, Feature feat) {
		return (this.features.contains(feat) && 
				featMap.containsKey(item) && featMap.get(item).containsKey(feat));
	}
	
	public boolean hasFeature(String name) {
		return (this.featureNames.containsKey(name));
	}

	public Set<Feature> features() {
		return Collections.unmodifiableSet(this.features);
	}
	
	public Set<String> featureNames() {
		return Collections.unmodifiableSet(this.featureNames.keySet());
	}

	/**
	 * Get the value for a item for a feature
	 * @param item
	 * @param feat
	 */
	public Value getValue(T item, Feature feat) {
		if (this.hasFeature(item, feat)) 
			return featMap.get(item).get(feat);
		else return null;
	}
	

	/**
	 * Gets the items that have a particular feature,
	 * or null if feature not here.
	 * @return
	 */
	public Set<T> get(Feature f) {
		if (!this.hasFeature(f)) return null;
		HashMap<Value, HashSet<T>> items = this.backMap.get(f);
		HashSet<T> got = new HashSet<T>();
		for (HashSet<T> set : items.values()) {
			got.addAll(set);
		}
		return got;
	}

	/**
	 * Gets the items that have this value.
	 * For CatSet values, get any item that intersects with the requested value(s).
	 * @param f
	 * @param v
	 * @return
	 */
	public Set<T> get(Feature f, Value v) {
		if (!this.hasFeature(f)) return null;
		HashMap<Value, HashSet<T>> items = this.backMap.get(f);
		if (items==null || v==null) {
			return null;
		}
		// for discrete or continuous, this is easy. but for categorical sets, it's a little trickier...
		if (!(v instanceof CatSet)) 
			return Collections.unmodifiableSet(items.get(v));
		else {
			HashSet<T> rets = new HashSet<T>();
			CatSet c = (CatSet) v;
			for (Value subv : c.getValue()) {
				rets.addAll(items.get(subv));
			}
			return Collections.unmodifiableSet(rets);
		}
	}

	/**
	 * Adds a feature to the necessary internal structures.
	 * @param feature
	 * @return
	 */
	protected boolean addFeature(Feature feature) {
		boolean ok = this.features.add(feature);		
		if (!ok) return false;

		this.featureNames.put(feature.name(), feature);

		this.backMap.put(feature, new HashMap<Value, HashSet<T>>());
		Value[] vs = feature.values();
		for (Value v : vs) {
			this.backMap.get(feature).put(v, new HashSet<T>());
		}

		return true;
	}
	
	public boolean replace(T orig, T replacement) {
		this.copyFeatures(orig, replacement);
		return this.remove(orig);
	}

	/**
	 * Copies all features from item 'orig' to item 'replacement'.
	 * Assert: 'orig' is in library and 'replacement' is not.
	 * @param orig
	 * @param replacement
	 * @return
	 */
	protected boolean copyFeatures(T orig, T replacement) {
		assert(this.contains(orig)) : 
			String.format("Trying to copy an item that does not exist in the library: %s", orig.toString());
		assert(this.contains(replacement)) : 
			String.format("Trying to copy features to an item " +
					"that already exists in the library: %s", replacement.toString());
		
		Map<Feature, Value> origVals=this.getFeatures(orig);
		return this.addValues(replacement, origVals);		
	}
	
	/**
	 * Removes item from the library.
	 * Usage example: when merging one or more items into a third new one.
	 * @param item
	 * @return true if item was removed (false if item not present)
	 */
	protected boolean remove(T item) {
		if (!this.contains(item)) return false;
			
		HashMap<Feature,Value> feats = this.featMap.remove(item);
		for (Entry<Feature,Value> entry : feats.entrySet()) {
			if (this.backMap.containsKey(entry.getKey()) 
					&& this.backMap.get(entry.getKey()).containsKey(entry.getValue())) {
				this.backMap.get(entry.getKey()).get(entry.getValue()).remove(item);
			}
			this.cleanup(item);			
		}
		return true;
	}
	
	/**
	 * Cleans up any other subclass information after removing an item from the library.
	 * @param item
	 */
	protected abstract void cleanup(T item);
	
	/**
	 * Tries to add the features for an item.
	 * 
	 * Returns false if we try to add a different value for a 
	 * previously-defined feature.
	 * @param item
	 * @param vals
	 * @return
	 */
	public boolean addValues(T item, Map<Feature, Value> vals) {
		// if edge present, check for overlap in feature names/values.
		boolean ok = true;
		if (this.contains(item)) {

			Map<Feature, Value> myFeats=this.getFeatures(item);
			ArrayList<Feature> newFeats = new ArrayList<Feature>(vals.keySet());
			for (Feature f : newFeats) {
				if (myFeats.containsKey(f)) {

					// did we already declare it and not a set-valued feature?
					if (vals.get(f) != myFeats.get(f) && !f.type().equals(Value.Type.SET)) {
						ok=false;
					}
					// if cat set, then merge values	
					else if (vals.get(f) != myFeats.get(f) && f.type().equals(Value.Type.SET)) {
						CatSet newVal = (CatSet) vals.get(f);
						CatSet curVal = (CatSet) myFeats.get(f);
						HashSet<Discrete> newSet = new HashSet<Discrete>(curVal.getValue());						
						newSet.addAll(newVal.getValue());

						vals.put(f, CatSet.makeValue(newSet));
					}
				}
			}
		}

		// compatible values?
		if (ok) {
			// okay! let's proceed.		
			for (Entry<Feature, Value> entry : vals.entrySet()) {
				// add feature if not already present
				boolean addFeature = this.addFeature(entry.getKey());

				// force OK because we checked already
				boolean ok2 = addValue(item, entry.getKey(), entry.getValue(), true);
				assert(ok2) : 
					String.format("Problem adding feature %s for edge %s", entry.getKey().name(), item);				
			}
		}


		return ok;		
	}

	/**
	 * Adds this feature value for this item.
	 * Private because we want to make sure in advance that
	 * we're not duplicating information or overwriting values willy-nilly.
	 * Assume feature was already added properly.
	 * 
	 * @param item
	 * @param f
	 * @param val
	 * @param force	to force-overwrite an existing value
	 * @return	false if exact feature already exists and value isn't same
	 */
	protected boolean addValue(T item, Feature f, Value val, boolean force) {
		assert(this.features.contains(f)) : 
			String.format("Feature %s hasn't been added already.", f.name());

		// return false if feature exists with different value (and not forcing)
		// can return true if feature exists with same value; no need to add.
		boolean overwriting=false;
		if (this.hasFeature(item, f)) {
			if (val != this.getValue(item, f) && !force) {
				return false;
			}
			if (val == this.getValue(item, f)) {
				return true;
			} 
			if (force) {
				overwriting=true;
			}
		}

		if (!this.contains(item)) {
			this.featMap.put(item, new HashMap<Feature, Value>());
		}
		// overwrites!
		Value add = this.featMap.get(item).put(f, val);

		if (add != null && !force) return false;		

		// now do in reverse
		if (!(val instanceof CatSet)) {
			HashMap<Value, HashSet<T>> subBack = this.backMap.get(f);
			assert( subBack!=null && val!=null) :
				"wtf";

			if (!subBack.containsKey(val)) {
				subBack.put(val, new HashSet<T>());
			}
			subBack.get(val).add(item);

			// if overwriting, remove old value ("add")
			if (overwriting) {
				subBack.get(add).remove(item);
			}
		}
		else {
			CatSet catval = (CatSet) val;
			for (Value v : catval.getValue()) {
				HashMap<Value, HashSet<T>> subBack = this.backMap.get(f);
				assert( subBack!=null && val!=null) :
					"wtf";

				if (!subBack.containsKey(v)) {
					subBack.put(val, new HashSet<T>());
				}
				subBack.get(v).add(item);

				// we don't overwrite values with catsets!
			}
		}
		return true;
	}	

	
	

}
