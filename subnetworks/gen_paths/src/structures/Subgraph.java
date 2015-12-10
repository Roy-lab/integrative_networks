package structures;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A subgraph is an unordered collection of edges and nodes.
 * Lets us input a collection of edges without forcing them awkwardly into 
 * directed paths. 
 * 
 * @author chasman
 *
 */
public class Subgraph {

	protected final String name;
	protected final Set<Edge> edges;
	protected final Set<String> nodes;
	protected final String toString;
		
	/**
	 * Named collection of edges and nodes.
	 * @param name
	 * @param edges
	 */
	public Subgraph(String name, Collection<String> nodes, Collection<Edge> edges) {
		this.name=name;
		this.edges = new HashSet<Edge>(edges);
		this.toString = this.buildString();
		this.nodes=new HashSet<String>(nodes);	
		for (Edge e : edges) {
			this.nodes.addAll(e.nodes());
		}
	}
	
	public Subgraph(String name, Collection<Edge> edges) {
		this.name=name;
		this.edges = new HashSet<Edge>(edges);
		this.toString = this.buildString();
		this.nodes=new HashSet<String>();
		for (Edge e : edges) {
			nodes.addAll(e.nodes());
		}
	}
	
	public String name() {
		return this.name;
	}
	
	public boolean contains(String node) {
		return this.nodes.contains(node);
	}
	
	public boolean contains(Edge e) {
		return this.edges.contains(e);
	}
	
	public Set<String> nodes() {
		return Collections.unmodifiableSet(nodes);
	}
	
	public Set<Edge> edges() {
		return Collections.unmodifiableSet(edges);
	}
	
	public String toString() {
		return this.toString;
	}
	
	/**
	 * Builds a string representation of the subgraph.
	 * @return
	 */
	protected String buildString() {
		StringBuilder sb = new StringBuilder(String.format("Subgraph %s", name));
		
		for (Edge e : this.edges) {			
			sb.append("\n\t");
			sb.append(e.i);
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
			sb.append(e.j);		
		}
		return sb.toString();
	}
	

}
