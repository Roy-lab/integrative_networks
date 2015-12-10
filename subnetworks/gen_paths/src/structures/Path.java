package structures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores an path in the network.
 * 
 * Paths are immutable so that we can store them in sets.
 * 
 * At the most basic, a path is an ordered list of nodes.
 * @author chasman
 *
 */
public class Path {
	
	/*
	 * What order is A with respect to B in this path? 
	 * @author chasman
	 *
	 */
	public enum PathOrder {
		ABOVE,	// A is above B
		BELOW,	// B is above A
		NA;	// One or both nodes not in the path - or, the path is a cycle, or branchy.
	}
	
	/*
	 * Ordered nodes and edges.
	 */
	protected final ArrayList<String> nodes;
	protected final ArrayList<Edge> edges;	
	protected String toString;
	protected HashSet<String> termini;
	
	/**
	 * Constructs an empty path.
	 * 
	 */
	protected Path() {
		this.nodes = new ArrayList<String>();
		this.edges = new ArrayList<Edge>();
		this.termini = new HashSet<String>();
		//this.toString = this.buildString();
	}
	
	/**
	 * Constructs a path with one item.
	 * @param node
	 */
	public Path(String node) {
		this.nodes = new ArrayList<String>();
		this.edges = new ArrayList<Edge>();
		this.nodes.add(node);
		this.toString = this.buildString();
		this.termini = new HashSet<String>();
		this.termini.add(node);
	}
	
	public Path(ArrayList<String> nodes, ArrayList<Edge> edges) {
		this.nodes=nodes;
		this.edges=edges;
		this.termini=new HashSet<String>();
		termini.add(nodes.get(nodes.size()-1));
		
		this.toString = this.buildString();		
	}
	
	/**
	 * Returns a copy of the path with the edge added.
	 * Returns null if the edge introduces a cycle or
	 * is not contiguous from the last edge.
	 * Assertion error if path is empty - the programmer should never be
	 * adding an edge to an empty path because the order of the nodes is then
	 * ambiguous.
	 * @param e
	 */
	public Path copyAdd(Edge e) {
		assert(this.nodes.size() > 0):
			"Trying to add an edge to an empty path. Don't do it.";
		
		// contiguity? one node must be the last one.
		String curLast = this.getNode(-1);
		
		String first="", second="";
		if (e.isDirected()) {
			first=e.i();
			second=e.j();
			
			// contiguity?
			if (!first.equals(curLast)) {
				return null;
			}
			
		}
		else {
			if (e.i().equals(curLast)) {
				first=e.i();
				second=e.j();
			} else if (e.j().equals(curLast)) {
				first=e.j();
				second=e.i();
			} else {
				// contiguity check
				return null;
			}
		}		
		
		assert(first.length()>0 && second.length()>0):
			"Trying to an an empty node to a path.";
			
		// acyclicity? the second node cannot be already in the path.
		if (this.nodes.contains(second)) {
			return null;
		}
			
		ArrayList<String> newN = new ArrayList<String>(this.nodes());
		ArrayList<Edge> newE = new ArrayList<Edge>(this.edges());
		
		newN.add(second);
		newE.add(e);
		return new Path(newN, newE);
	}
	
	/**
	 * Checks what order these nodes fall in this path.
	 * You should really never call this on the same node -
	 * if you do, you'll get an assertion error for your troubles.
	 * @param a
	 * @param b
	 * @return
	 */
	public PathOrder order(String a, String b) {
		int ai = this.nodes.indexOf(a);
		int bi = this.nodes.indexOf(b);		
		
		// if either not present return NA
		if (ai < 0 || bi < 0) return PathOrder.NA;
		
		assert(ai != bi) :
			"Called Path.order on the same node.";
		
		if (ai < bi) return PathOrder.ABOVE;
		if (bi < ai) return PathOrder.BELOW;
		else return PathOrder.NA;
	}

	
	/**
	 * Gets the node at position i in the path.
	 * Allows requests for negative indices 
	 * @param i		-len(path)-1 < i < len(path)
	 * @return	node at position i
	 */
	public String getNode(int i) {
		// allow arraylist to take care of out-of-bounds exceptions
		// negative index? count backwards
		if (i < 0) {
			return nodes.get(nodes.size()+i);
		}
		return nodes.get(i);
	}
	
	/**
	 * Gets the edge at position i.
	 * Allows negative indices.
	 * @param i		-len(path)-1 < i < len(path)
	 * @return	edge at position i
	 */
	public Edge getEdge(int i) {
		if (i < 0) {
			return edges.get(edges.size()+i);
		}
		return edges.get(i);
	}
	
	/**
	 * Returns the LENGTH of the path in edges.
	 * For branchy paths, this will return the length of the linear portion,
	 * not the total number of edges + terminal edges.
	 * @return
	 */
	public int edgeLength() {
		return this.edges.size();
	}
	
	public List<String> nodes() {
		return Collections.unmodifiableList(this.nodes);
	}
	
	public List<Edge> edges() {
		return Collections.unmodifiableList(this.edges);
	}
	
	/**
	 * Returns the termini. For regular linear paths, this is just one.
	 * But this lets us interact with Paths and BranchyPaths in the same way
	 * when we want to retrieve the final node(s).
	 * @return
	 */
	public Set<String> termini() {
		return Collections.unmodifiableSet(this.termini);
	}
	
	/**
	 * Returns the terminal edge. For regular linear paths, this is just one.
	 * But this lets us interact with Paths and BranchyPaths in the same way
	 * when we want to retrieve the final node(s).
	 * @return
	 */
	public Set<Edge> terminalEdges() {
		HashSet<Edge> term= new HashSet<Edge>();
		term.add(this.getEdge(-1));
		return Collections.unmodifiableSet(term);
	}
	
	/**
	 * Returns a copy of this path.
	 * @return
	 */
	public Path copy() {
		Path copy = new Path(new ArrayList<String>(this.nodes), new ArrayList<Edge>(this.edges));
		return copy;
	}
	
	/**
	 * Provides a string representation of the path.
	 * -> for directed edges, -- for undirected.
	 */
	public String toString() {				
		return this.toString;
	}
	
	protected String buildString() {
		StringBuilder sb = new StringBuilder(this.getNode(0));
		// one fewer edge than nodes
		for (int i=1; i < nodes.size(); i++) {
			String nextN = nodes.get(i);
			Edge e = edges.get(i-1);
			// possibilities
			if (!e.directed) {
				switch(e.sign) {
				case UNKNOWN: sb.append(" -- "); break;
				case POSITIVE: sb.append(" <--> "); break;
				case NEGATIVE: sb.append(" |--| "); break;
				}
			}
			else if (e.directed) {
				switch(e.sign) {
				case UNKNOWN: sb.append(" --+ "); break;
				case POSITIVE: sb.append(" --> "); break;
				case NEGATIVE: sb.append(" --| "); break;
				}
			}				
			
			sb.append(nextN);			
		}
		return sb.toString();
	}
	
	/**
	 * Compares paths based on string representation.
	 */
	public boolean equals(Object other) {
		if (!(other instanceof Path)) return false;
		return this.toString().equals(other.toString());
	}
	
	public int hashCode() {
		return this.toString().hashCode();
	}

}
