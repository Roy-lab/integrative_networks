package structures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import utilities.StringUtils;

/**
 * A branchy path can have multiple termini.
 * @author chasman
 *
 */
public class BranchyPath extends Path {

	protected final HashSet<String> termini;	
	protected final HashSet<Edge> terminalEdges;
	
	protected BranchyPath() {
		this.termini = new HashSet<String>();
		this.terminalEdges = new HashSet<Edge>();
	}

	
	/**
	 * Constructs a branchy path from the body of a linear path
	 * plus a set of terminal edges and nodes.
	 * @param body
	 * @param termini
	 * @param termEdges
	 */
	public BranchyPath(Path body,
			Set<String> termini, Set<Edge> termEdges) {
		this();
		this.nodes.addAll(body.nodes);
		this.edges.addAll(body.edges);
		this.termini.addAll(termini);
		this.terminalEdges.addAll(termEdges);
		this.toString = this.buildString();
	}
	
	
	/**
	 * Gets the node at position i in the path.
	 * Special case: If terminal nodes exist, throw exception if you try to
	 * access the "last" node.
	 * Allows requests for negative indices 
	 * @param i		-len(path)-1 < i < len(path)
	 * @return	node at position i
	 */
	public String getNode(int i) {
		// allow arraylist to take care of out-of-bounds exceptions
		// negative index? count backwards
		int index = i;
		if (i < 0) {
			// branchy paths - termini aren't counted in "nodes", so add another.
			index = (nodes.size()+i+1);
		}
			
		// generally, accessing len-1 of branchy path is not a good idea
		// only exception is when the path has no linear portion: it's just a regulator with its branchy targets. 
		if (i==nodes.size()-1 && nodes.size() > 1 ) {
			throw new RuntimeException("Tried to access last item of a BranchyPath; not unique.");
		}
		return nodes.get(index);
	}
		
	/**
	 * Gets the edge at position i.
	 * Allows negative indices.
	 * Special case: If terminal nodes exist, throw exception if you try to
	 * access the "last" edge (-1 or edge length)
	 * @param i		-len(path)-1 < i < len(path)
	 * @return	edge at position i
	 */
	public Edge getEdge(int i) {
		int index = i;
		if (i < 0) {
			index =edges.size()+i;
		}
		if (i==this.edgeLength()-1) {
			throw new RuntimeException("Tried to access last edge of a BranchyPath; not unique.");
		}
		return edges.get(index);
	}

	/**
	 * Returns the LENGTH of the path in edges.
	 * For branchy paths, this will return the length of the linear portion plus 1,
	 * not the total number of edges + terminal edges.
	 * @return
	 */
	@Override
	public int edgeLength() {
		// length of linear chain + 1 if termini		
		int len = this.edges.size() + (this.terminalEdges.size() > 0 ? 1 : 0);
		return len;
	}
	
	/**
	 * Nodes - return ALL nodes in path, including multiple termini.
	 */
	@Override
	public List<String> nodes() {
		ArrayList<String> allNodes = new ArrayList<String>(this.nodes);
		allNodes.addAll(termini);
		return allNodes;
	}
	
	/**
	 * Returns ALL edges, including terminal edges.
	 */
	@Override
	public List<Edge> edges() {
		ArrayList<Edge> allEdges = new ArrayList<Edge>(this.edges);
		allEdges.addAll(this.terminalEdges);
		return allEdges;
	}
	
	/**
	 * Returns the terminal edges only.
	 * @return
	 */
	public Set<Edge> terminalEdges() {
		return Collections.unmodifiableSet(this.terminalEdges);
	}
	
	/**
	 * Returns the terminal nodes only.
	 * @return
	 */
	public Set<String> termini() {
		return Collections.unmodifiableSet(this.termini);
	}
	
	/**
	 * Returns a copy of this path.
	 * @return
	 */
	public Path copy() {
		Path copy = new BranchyPath(bodyPath(),
				new HashSet<String>(this.termini), 
				new HashSet<Edge>(this.terminalEdges));
		return copy;
	}
	
	/**
	 * Returns the linear portion of this path as a new Path.
	 * @return
	 */
	public Path bodyPath() {
		return new Path(this.nodes, this.edges);
	}	
	
	/**
	 * Builds the string representation of the BranchyPath.
	 */
	protected String buildString() {
		assert (this.nodes.size() >= this.edges.size()) : "Missed node while adding edge?";
		if (this.nodes.size() == 0) {
			return "";
		}
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
		// add count of targets 
		sb.append(String.format(" --+ [%d termini (%s)]", this.termini.size(), StringUtils.join(this.termini, "|")));
		
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
	
	/**
	 * Checks what order these nodes fall in this path.
	 * You should really never call this on the same node -
	 * if you do, you'll get an assertion error for your troubles.
	 * If both nodes are termini, then order == NA.
	 * If one node is terminal and other isn't, then order.
	 * @param a
	 * @param b
	 * @return
	 */
	public PathOrder order(String a, String b) {
		
		int ai = this.nodes.indexOf(a);
		int bi = this.nodes.indexOf(b);		
		
		boolean aTerm = (ai < 0 && this.termini.contains(a));
		boolean bTerm = (bi < 0 && this.termini.contains(b));
		
		
		// if either not present OR both are termini, return NA
		if (ai < 0 && !aTerm || bi < 0 && ! bTerm 
				|| (aTerm && bTerm)) {
			return PathOrder.NA;
		}
		
		assert(ai != bi) :
			"Called Path.order on the same node.";
		
		if (ai < bi) return PathOrder.ABOVE;
		if (bi < ai) return PathOrder.BELOW;
		else return PathOrder.NA;
	}
	
}
