package structures;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import filters.EdgeFilterManager;

import utilities.Enums.AddEdgeMode;

/**
 * A PathManager holds onto a set of paths and lets us do things 
 * like look up the paths for each node.
 * 
 * Since paths are implemented as immutable, we store them in sets here.
 * 
 * 
 * @author chasman
 *
 */
public class PathManager {
	
	/*
	 * Now we have a default label for paths.
	 */
	public static final String DEFAULT="DEFAULT";

	/*
	 * List of paths.
	 */
//	protected HashSet<Path> allPaths;
	
	/*
	 * Map from nodes to paths.
	 */
	protected HashMap<String, HashSet<Path>> nodesToPaths;
	
	/*
	 * Labels for paths. 
	 */
	protected HashMap<Path, HashSet<String>> pathLabels;
	protected HashMap<String, HashSet<Path>> labelsToPaths;
	
		
	/**
	 * Constructs an empty PathManager.
	 */
	public PathManager() {
	//	this.allPaths=new HashSet<Path>();		
		this.nodesToPaths=new HashMap<String,HashSet<Path>>();
		this.pathLabels= new HashMap<Path, HashSet<String>>();
		this.labelsToPaths= new HashMap<String, HashSet<Path>>();
		
	}
	
	/**
	 * Constructs a PathManager with a set of paths and a label for those paths.
	 * @param paths
	 */
	public PathManager(Collection<Path> paths, String label) {
		this();
		this.addAll(paths, label);	
	}
	
	
	/**
	 * Imports the contents of another PathManager into this one.
	 * Warning - does no kind of checking for duplicates.
	 * @param paths
	 */
	public void addAll(PathManager pathMan) {
		for (String label : pathMan.allLabels()) {
			this.addAll(pathMan.getPathsForLabel(label), label);
		}				
	}
	
	
	/**
	 * Adds a collection of paths.
	 * @param paths
	 */
	public void addAll(Collection<Path> paths, String label) {
		for (Path p : paths) {
			this.add(p, label);
		}
	}
	
	/**
	 * Adds a path (with label!) to the PathManager.
	 * @param p
	 */
	public void add(Path p, String label) {
		boolean has = this.contains(p);
		
		// add label
		if (!this.pathLabels.containsKey(p)) pathLabels.put(p, new HashSet<String>());
		this.pathLabels.get(p).add(label);
		
		if (!this.labelsToPaths.containsKey(label)) labelsToPaths.put(label, new HashSet<Path>());
		this.labelsToPaths.get(label).add(p);
		

		// already catalogued? if so, just add the label and be done.
		if (has) return;
		
		// add path for each node
		for (String node : p.nodes()) {
			if (!this.nodesToPaths.containsKey(node)) {
				this.nodesToPaths.put(node, new HashSet<Path>());
			}
			this.nodesToPaths.get(node).add(p);
		}
	}
	
	/**
	 * Returns a view into the set of all paths.
	 * @return
	 */
	public Set<Path> allPaths() {
		return Collections.unmodifiableSet(this.pathLabels.keySet());
	}	
	
	/**
	 * Returns a reduced path manager in which we've removed paths
	 * that begin with the supplied hits and which were found
	 * from a pathfinder with a supplied label.
	 * Use case: hide the hit-interface paths beginning with
	 * a subset of hits.
	 * 
	 * HACKY
	 * @param toHide
	 * @return
	 */
	public PathManager reduce(Set<String> toHide, String label) {
		PathManager newPM = new PathManager();
		for (Entry<String, HashSet<Path>> entry : this.labelsToPaths.entrySet()) {
			// matching label? filter.
			if (entry.getKey().equals(label)) {
				for (Path p : entry.getValue()) {
					// only keep paths that don't begin with hidden hits
					if (!toHide.contains(p.getNode(0))){
						newPM.add(p, label);
					}
				}
			} else {
				// add all
				newPM.addAll(entry.getValue(), entry.getKey());
			}
		}
		return newPM;
	}
	
	/**
	 * Returns the number of paths stored.
	 * @return
	 */
	public int size() {
		return this.pathLabels.size();
	}
	
	/**
	 * Returns the number of nodes in these paths.
	 * @return
	 */
	public int nodeCount() {
		return this.nodesToPaths.keySet().size();
	}
	
	/**
	 * Returns true if there are any paths stored for this node.
	 * @param node
	 * @return
	 */
	public boolean contains(String node) {
		return this.nodesToPaths.containsKey(node);
	}
	
	/**
	 * Returns true if the node has been indexes.
	 * @param p
	 * @return
	 */
	public boolean contains(Path p) {
		return this.pathLabels.containsKey(p);
	}
	
	/**
	 * Gets the paths stored for this node. 
	 * Returns null if the node is not present.
	 * @param node
	 * @return
	 */
	public Set<Path> getPaths(String node) {
		if (!this.nodesToPaths.containsKey(node)) return null;
		else return Collections.unmodifiableSet(this.nodesToPaths.get(node));
	}
	
	/**
	 * Gets the set of paths having a particular label.
	 * @param label
	 * @return
	 */
	public Set<Path> getPathsForLabel(String label) {
		return this.labelsToPaths.get(label);
	}
	
	/**
	 * Gets the labels given to a path.
	 * @param p
	 * @return
	 */
	public Set<String> getLabels(Path p) {
		return this.pathLabels.get(p);
	}
	
	public Set<String> allLabels() {
		return Collections.unmodifiableSet(this.labelsToPaths.keySet());
	}
	
	/**
	 * Applies an edge filter manager on each path in the manager.
	 * Returns a new path manager consisting only of paths that are accepted.
	 * @param efm
	 * @return
	 */
	public static PathManager applyEdgeFilter(PathManager manager, EdgeFilterManager efm) {
		PathManager accepted = new PathManager();
		for (String label : manager.labelsToPaths.keySet()) {
			for (Path p : manager.getPathsForLabel(label)) {
				boolean accept = efm.accept(p.edges());
				if (accept) {
					accepted.add(p, label);
				} else {
					// debug
					//System.out.println("rejected a path");
				}
			}
		}
		return accepted;
	}
	
	/**
	 * Makes a graph out of the paths in a PathManager.
	 * @param man
	 * @return
	 */
	public static Graph makeGraph(PathManager man) {
		Graph g = new Graph();
		for (Path p : man.allPaths()) {
			g.addAll(p.edges());
		}
		return g;
	}
	
	/**
	 * Given a PathManager, filter a set of edges according to the 
	 * specified AddEdgeMode.
	 * For example: we may want to return only edges for which the target 
	 * (or source) is in the PathManager.
	 * For undirected edges, either target or source will be sufficient.
	 * @param man
	 * @param edges
	 * @param mode
	 * @return
	 */
	public static Collection<Edge> filterEdges(PathManager man, 
			Collection<Edge> edges, AddEdgeMode mode) {
		if (mode==AddEdgeMode.ALL) return edges;
		HashSet<Edge> retEdges = new HashSet<Edge>();
		for (Edge e : edges) {
			if (mode == AddEdgeMode.SOURCE_IN_PATH || !e.isDirected()) {
				if (man.contains(e.i())) retEdges.add(e);
			}
			if (mode == AddEdgeMode.TARGET_IN_PATH || !e.isDirected()) {
				if (man.contains(e.j())) retEdges.add(e);
			}
		}
		return retEdges;
	}
	
	/**
	 * Given a PathManager, an AddEdgeMode, and a set of edges,
	 * associate edges with paths  according to the AddEdgeMode.
	 * Like filterEdges, but returns edges indexed by the path that
	 * brings them in.
	 * 
	 * @param man
	 * @param edges
	 * @param mode
	 * @return
	 */
	public static HashMap<Path, HashSet<Edge>> filterSubgraphEdgesByPath(
			PathManager man, Collection<Edge> edges, AddEdgeMode mode) {
		HashMap<Path, HashSet<Edge>> index = new HashMap<Path, HashSet<Edge>>();
		
		// for each edge, get the paths
		for (Edge e : edges) {
			// we care about the source's paths
			if (mode == AddEdgeMode.SOURCE_IN_PATH || !e.isDirected()) {
				Set<Path> epaths = man.getPaths(e.i());
				if (epaths == null) continue;
				for (Path p : epaths) {
					if (!index.containsKey(p)) {
						index.put(p, new HashSet<Edge>());
					}
					index.get(p).add(e);
				}
			}
			// we care about the target's paths
			if (mode == AddEdgeMode.TARGET_IN_PATH || !e.isDirected()) {
				// got paths?
				Set<Path> epaths = man.getPaths(e.j());
				if (epaths == null) continue;
				for (Path p : epaths) {
					if (!index.containsKey(p)) {
						index.put(p, new HashSet<Edge>());
					}
					index.get(p).add(e);
				}
			}
		}
		return index;
	}
	
}
