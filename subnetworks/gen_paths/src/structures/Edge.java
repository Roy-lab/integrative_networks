package structures;

import java.util.HashSet;
import java.util.Set;

import utilities.Enums.Sign;

/**
 * An edge is a pair of nodes (strings) that may be directed.
 * Immutable once made!
 * Compare based on string representation.
 * @author chasman
 *
 */

public class Edge implements Comparable<Edge>{
	
	/*
	 * Nodes
	 */
	protected final String i, j, key;
	
	// is the edge directed?
	protected final boolean directed;
	
	// does it have a sign?
	protected Sign sign;
	
	/**
	 * Creates an edge between nodes i and j. 
	 * May have a direction and/or a sign.
	 * @param i
	 * @param j
	 * @param directed
	 * @param sign
	 */
	public Edge(String i, String j, boolean directed, Sign sign) {
		this.i=i;
		this.j=j;
		this.directed=directed;
		this.sign=sign;
		this.key=makeKey();
	}		
	
	/**
	 * Creates an edge between nodes i and j.
	 * If directed=false, then the edge is created as an undirected
	 * edge with 'canonical' direction i->j.
	 * 
	 * @param i	source if directed=true
	 * @param j	target if directed=true 
	 * @param directed	if true, edge is directed i->j
	 */
	public Edge(String i, String j, boolean directed) {
		this(i,j,directed,Sign.UNKNOWN);
	}	
	
	/**
	 * Creates an undirected edge between nodes i and j. 
	 * May have a sign.
	 * @param i
	 * @param j
	 * @param sign
	 */
	public Edge(String i, String j, Sign sign) {
		this(i,j,false,sign);	
	}
	
	private String makeKey() {
		String d = directed? "d" : "u";
		return String.format("%s.%s.%s.%s", this.i,this.j, d, this.sign.abbrev());
	}
	
	/** 
	 * Copies edge, replacing original node with a new one.
	 * Returns null edge doesn't contain node.
	 * @param orig
	 * @param replace
	 * @return
	 */
	public Edge replaceNode(String orig, String rep) {
		if (!this.nodes().contains(orig)) {
			return null;
		}
		Edge replacement=null;
		if (this.i().equals(orig)) {
			replacement = new Edge(rep, this.j(), this.isDirected(), this.sign());
		} else if (this.j().equals(orig)) {
			replacement = new Edge(this.i(), rep, this.isDirected(), this.sign());
		}
		return replacement;
	}
	
	/**
	 * Returns true if the edge is directed.
	 * @return
	 */
	public boolean isDirected() {
		return this.directed;
	}
	
	public Sign sign() {
		return this.sign;
	}
	
	/**
	 * Returns true if i==j.
	 * @return
	 */
	public boolean isSelfLoop() {
		return this.i.equals(this.j);
	}
	
	/**
	 * Returns the first node in the edge according
	 * to the canonical direction.
	 * @return
	 */
	public String i() {
		return i;
	}
	
	/**
	 * Returns the second node in the edge according
	 * to the canonical direction. 
	 * @return
	 */
	public String j() {
		return j;
	}


	/**
	 * Returns the nodes in this edge.
	 * @return
	 */
	public Set<String> nodes() {
		HashSet<String> nodes = new HashSet<String>();
		nodes.add(i);
		nodes.add(j);
		return nodes;
	}
	
	/**
	 * Checks to see if node a is the source of this edge.
	 * For undirected edges, returns true if the node is in the edge at all.
	 * 
	 * @return true if a is source OR a is in the edge and the edge is undirected.
	 */
	public boolean isSource(String a) {
		if (this.directed) {
			return (a.equals(this.i));
		} else {
			return (a.equals(this.i) || a.equals(this.j));
		}
	}
	
	/**
	 * Checks to see if node a is the target of this edge.
	 * For undirected edges, returns true if the node is in the edge.
	 * 
	 * @return true if a is target OR a is in the edge and the edge is undirected.
	 */
	public boolean isTarget(String a) {
		if (this.directed) {
			return (a.equals(this.j));
		} else {
			return (a.equals(this.i) || a.equals(this.j));
		}
	}
	
	/**
	 * Returns the unique string defining this edge:
	 * i.j.d.S for directed edges (i->j)
	 * or i.j.u.S for undirected edges (i--j)
	 * where S is either a (activating) or h (inhibiting)
	 * @return
	 */
	@Override
	public String toString() {
		return this.key;
	}
	

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Edge)) return false;
		return this.toString().equals(other.toString());
	}

	/**
	 * Compare to other edges based on the order of their
	 * string keys.
	 */
	@Override
	public int compareTo(Edge other) {
		return this.key.compareTo(other.key);
	}
	
	public int hashCode() {
		return this.key.hashCode();
	}
	

}
