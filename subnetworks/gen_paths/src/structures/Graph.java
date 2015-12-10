package structures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import utilities.StringUtils;
import exceptions.DuplicateException;

/**
 * Represents a graph as an incidence map.
 * @author chasman
 *
 */
public class Graph {

	/*
	 * Relationship between an edge and a node.
	 */
	public enum RType { INCOMING, OUTGOING, UNDIRECTED};

	/*
	 * Stores in both directions for directed edges.
	 * Second inner hashmap splits on "incoming", "outgoing", and "undirected".
	 * Third inner hashmap holds the actual edges. May be multiple signs between
	 * same nodes!
	 * 
	 * Ugh, maybe just keeping three maps would have been better? Unclear.
	 */
	protected HashMap<String, HashMap<RType, HashMap<String, HashSet<Edge>>>> graph;

	/**
	 * Makes a new graph.
	 */
	public Graph() {
		this.graph = new HashMap<String, HashMap<RType, HashMap<String, HashSet<Edge>>>>();
	}

	public Graph(Set<Edge> edges) {
		this();
		this.addAll(edges);
	}

	public Set<String> nodes() {
		return Collections.unmodifiableSet(this.graph.keySet());
	}

	/**
	 * Returns true if the node is in the graph.
	 * @param node
	 * @return
	 */
	public boolean contains(String node) {
		return graph.containsKey(node);
	}

	/**
	 * Copies a graph. 
	 * @return
	 */
	public Graph copy() {
		Graph newG = new Graph(this.edges());		
		return newG;		
	}	

	/**
	 * Returns a new graph consisting of all edges between a given set of nodes.
	 * @param nodes
	 * @return
	 */
	public Graph restrict(Set<String> nodes) {
		Graph newG = new Graph();

		for (String a : nodes) {
			newG.add(a);
			for (String b : nodes) {
				Set<Edge> adj = this.adjacent(a,b);
				if (adj != null) newG.addAll(adj);
			}
		}

		return newG;	
	}

	/**
	 * Returns a copy of the graph in which nodes without edges have been
	 * removed.
	 * @return
	 */
	public Graph removeEdgeless() {
		Graph newG = this.copy();
		for (String s : this.nodes()) {
			if (this.incident(s).size() == 0) newG.remove(s);
		}
		return newG;
	}

	/**
	 * Returns a copy of the graph in which self-loops have been removed.
	 * removed.
	 * @return
	 */
	public Graph removeSelfLoops() {
		Graph unselfish = new Graph();

		for (Edge e : this.edges()) {
			if (!e.isSelfLoop()) unselfish.add(e);
		}
		return unselfish;
	}

	/**
	 * Returns true if the edge exists.
	 * @param edge
	 * @return
	 */
	public boolean contains(Edge edge) {
		if (!this.contains(edge.i) || !this.contains(edge.j)) return false;
		boolean found=false;
		for (RType type : this.get(edge.i).keySet()) {
			found = this.get(edge.i, type).get(edge.j).contains(edge);
			if (found) break;
		}
		return (found);
	}

	/**
	 * Retrieves all edges. We make this one anew each time, because
	 * it gets updated by additions/removals from the graph.
	 * Use this one sparingly, okay?
	 * @return	a set of all the edges in this graph
	 */
	public Set<Edge> edges() {
		HashSet<Edge> edges = new HashSet<Edge>();
		for (String n : this.nodes()) {
			edges.addAll(this.incident(n));
		}
		return edges;
	}

	/**
	 * Gets the count of all neighbors.
	 * @param node
	 * @return
	 */
	public int degree(String node) {
		return this.neighbors(node).size();
	}

	/**
	 * Gets the node's degree, restricted to a type.
	 * @param node
	 * @param type
	 * @return
	 */
	public int degree(String node, RType type) {
		try {
			return this.neighbors(node,type).size();
		} catch (NullPointerException npe) {
			return 0;
		}
	}

	/**
	 * Return all edges between a pair of nodes.
	 * @param a
	 * @param b
	 * @return	edge or null
	 */
	public Set<Edge> adjacent(String a, String b) {
		HashSet<Edge> found=new HashSet<Edge>();
		if (this.contains(a) && this.contains(b)) {
			for (RType type : this.graph.get(a).keySet()) {
				HashMap<String, HashSet<Edge>> edgeMap = this.get(a, type);
				if (edgeMap.containsKey(b)) {
					found.addAll(edgeMap.get(b));
				}
			}
		}
		return Collections.unmodifiableSet(found);
	}

	/**
	 * Returns all neighbors of node a.
	 * @param a
	 * @return
	 */
	public Set<String> neighbors(String a) {
		Set<Edge> incident = this.incident(a);
		HashSet<String> neighbors = new HashSet<String>();
		for (Edge e : incident) {
			assert(e.nodes().contains(a)) : "Node not in incident edge? Weird.";

			if (!e.i().equals(a)) neighbors.add(e.i());
			else if (!e.j().equals(a)) neighbors.add(e.j());
		}
		return neighbors;
	}

	/**
	 * Gets all neighbors of a, with a particular relationship 
	 *
	 * @param a	node of interest
	 * @param type	type of relationship
	 * @return
	 */
	public Set<String> neighbors(String a, RType type) {
		if (!this.contains(a)) return null;
		Set<Edge> incident = this.incident(a, type);
		HashSet<String> nodes = new HashSet<String>();
		for (Edge e : incident) {
			// get the other node.  
			Set<String> enodes = e.nodes();
			enodes.remove(a);

			// if self-loop, then add self to list.
			if (enodes.size()==0) nodes.add(a);
			else {
				nodes.addAll(enodes);
			}
		}
		return nodes;
	}

	/**
	 * Returns a new set containing a type of edges incident to this node
	 * (of a particular nature - incoming, outgoing, undirected).
	 * @param a
	 * @param type
	 * @return	null if node not in graph; new set of edges otherwise
	 */
	public Set<Edge> incident(String a, RType type) {
		// node not in graph at all? null;
		if (!this.contains(a)) return null;		

		HashSet<Edge> incident = new HashSet<Edge>();
		for (HashSet<Edge> nei : this.graph.get(a).get(type).values()) {
			incident.addAll(nei);
		}
		return incident;
	}

	/**
	 * Returns ALL edges incident to this node.
	 * @param a
	 * @param type
	 * @return
	 */
	public Set<Edge> incident(String a) {
		Set<Edge> incident = new HashSet<Edge>();	


		// node not in graph at all? empty set;
		if (!this.contains(a)) return incident;

		for (RType type : this.graph.get(a).keySet()) {
			for (String b : this.graph.get(a).get(type).keySet()) {
				HashSet<Edge> found = this.graph.get(a).get(type).get(b);
				incident.addAll(found);				
			}
		}
		return incident;
	}


	/**
	 * Adds an edge to the graph.
	 * Like HashSet, if there's already an edge here,
	 * we won't do anything and return false.
	 * If we add the edge, return true;
	 * @param el
	 * @return
	 */
	public boolean add(Edge edge) {
		// if either node is not already present, set up
		// the graph data structure for them
		if (!graph.containsKey(edge.i())) {			
			graph.put(edge.i(), getInnerMapTemplate());
		}		
		if (!graph.containsKey(edge.j())) {
			graph.put(edge.j(), getInnerMapTemplate());
		}

		// relationship between the edge and each node
		RType iType = edge.directed ? RType.OUTGOING : RType.UNDIRECTED;
		RType jType = edge.directed ? RType.INCOMING : RType.UNDIRECTED;

		// add to I
		if (!graph.get(edge.i()).get(iType).containsKey(edge.j())) {
			graph.get(edge.i()).get(iType).put(edge.j(), new HashSet<Edge>());
		}
		boolean repI = graph.get(edge.i()).get(iType).get(edge.j()).add(edge);

		if (!graph.get(edge.j()).get(jType).containsKey(edge.i())) {
			graph.get(edge.j()).get(jType).put(edge.i(), new HashSet<Edge>());
		}
		boolean repJ = graph.get(edge.j()).get(jType).get(edge.i()).add(edge);

		// problem if repI != repJ and not a self-loop
		assert( (repI == repJ) || edge.isSelfLoop()) : 
			String.format("Graph imbalanced. Tried to add edge %s, but " +
					"one direction was already there.");

		// return false if already present
		return repI;
	}

	/**
	 * Adds many edges to the graph. 
	 * If any connection is already present, we'll be overwriting the
	 * existing one. 
	 * @param edges
	 */
	public void addAll(Collection<Edge> edges) {
		for (Edge e : edges) {
			this.add(e);
		}
	}


	/**
	 * For some reason we may want to add a node without any edges.
	 * @param n
	 * @return	false if node already present.
	 */
	public boolean add(String n) {
		if (graph.containsKey(n)) return false;
		HashMap<RType, HashMap<String, HashSet<Edge>>> template = getInnerMapTemplate();
		graph.put(n, template);
		return true;
	}

	/**
	 * Remove a node and its incident edges. Return its incident edge set.
	 * @param n
	 * @return
	 */
	public Set<Edge> remove(String a) {
		if (!this.contains(a)) return null;

		Set<Edge> incident = this.incident(a);
		for (Edge e : incident) {
			this.remove(e);
		}

		// get a copy of the edges of this node
		//		HashMap<RType, HashMap<String, HashSet<Edge>>> toRem = this.graph.get(a);
		//				
		//		for (RType type : toRem.keySet()) {
		//			for (HashSet<Edge> edges : toRem.get(type).values()) {
		//				for (Edge e : edges) {
		//					
		//					this.remove(e);		
		//				}
		//			}
		//		}	


		// remove node and its perspective on the adjacent edges
		HashMap<RType, HashMap<String, HashSet<Edge>>> rem = this.graph.remove(a);

		return incident;
	}

	/**
	 * Remove an edge from the graph. (Nodes remain.)
	 * @param e
	 * @return	the removed edge, or null
	 */
	public Edge remove(Edge e) {
		if (!this.contains(e.i) || !this.contains(e.j)) 
			return null;

		// try to remove the edge from both directions.
		// increment 'found' when one version of the edge is located and
		// removed. can stop when both are. 
		// (this still applies to self-loops and directed edges,
		// but not undirected self-loops.)
		int found = 0;
		for (RType type : RType.values()) {
			boolean rem = this.get(e.i, type).containsKey(e.j) && this.get(e.i, type).get(e.j).remove(e);
			if (rem) found+=1;

			boolean rem2 = this.get(e.j, type).containsKey(e.i) && this.get(e.j, type).get(e.i).remove(e);			
			if (rem2) found +=1;		

			if (found == 2 || (found==1 && !e.isDirected() && e.isSelfLoop())) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Creates a graph from an edge library.
	 * 
	 * @param el
	 * @param overrideFeat
	 * @return
	 */
	public static Graph createFromEdgeLibrary(EdgeLibrary el, String override)
			throws DuplicateException {
		Graph g = new Graph();

		// start with all edges
		HashSet<Edge> remaining=new HashSet<Edge>(el.items());
		
		if (override != null) {
			Feature overrider=el.getFeature(override);

			HashSet<Edge> remEdge=new HashSet<Edge>();
			Set<Edge> overEdge=el.get(overrider);
			for (Edge e : overEdge) {
				HashSet<Edge> toRemove=el.containsConnection(e);
				remEdge.addAll(toRemove);
			}

			//remove the identified edges 
			remaining.removeAll(remEdge);
			// add the overriding edges
			remaining.addAll(overEdge);	
		}

		for (Edge e :remaining) {
			g.add(e);
		}	

		return g;
	}



	/**
	 * Creates a graph from an edge library.
	 * Does not override any edges.
	 * @param el
	 * @return
	 */
	public static Graph createFromEdgeLibrary(EdgeLibrary el)
			throws DuplicateException {
		return createFromEdgeLibrary(el, null);
	}

	/**
	 * Shorthand
	 * @param a
	 * @return
	 */
	protected HashMap<RType, HashMap<String, HashSet<Edge>>> get(String a) {
		return this.graph.get(a);
	}

	/**
	 * Shorthand for retrieving the inner inner map!
	 * @param a
	 * @param type
	 * @return
	 */
	protected HashMap<String,HashSet<Edge>> get(String a, RType type) {
		if (!this.contains(a)) return null;
		return this.graph.get(a).get(type);
	}

	/**
	 * Get the template for the inner map in the graph.
	 * @return
	 */
	private HashMap<RType, HashMap<String, HashSet<Edge>>> getInnerMapTemplate() {
		HashMap<RType, HashMap<String, HashSet<Edge>>> template = 
				new HashMap<RType, HashMap<String, HashSet<Edge>>>();
		for (RType type : RType.values()) {
			template.put(type, new HashMap<String, HashSet<Edge>>());
		}
		return template;
	}	

	/**
	 * Returns a string that shows the neighbor and edge
	 * structure. For testing purposes - this will get overwhelming.
	 * @return
	 */
	public String graphNeighborStructure() {
		StringBuilder sb = new StringBuilder();
		ArrayList<String> nodes = new ArrayList<String>(this.nodes());
		Collections.sort(nodes);
		for (String n : nodes) {
			sb.append(n + "\n");
			for (RType type : Graph.RType.values()) {
				Set<String> neigh = this.neighbors(n, type);
				Set<Edge> edges = this.incident(n, type);
				String edgeStr = StringUtils.sortJoin(edges, ", ");
				sb.append(String.format("\t%s\t[%s]\t[%s]\n", 
						type, 
						StringUtils.sortJoin(neigh, ", "),
						edgeStr));
			}
		}	
		return sb.toString();
	}

	public String toString() {
		return this.graphNeighborStructure();
	}

}
