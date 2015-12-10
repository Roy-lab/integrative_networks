package utilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import structures.Continuous;
import structures.Discrete;
import structures.Edge;
import structures.EdgeLibrary;
import structures.Feature;
import structures.Graph;
import structures.NodeLibrary;
import structures.Value;
import filters.Filter;

/**
 * Contains methods for doing basic stuff with graphs, like filtering,
 * etc.
 * @author chasman
 *
 */
public class GraphUtils {
	
	/*
	 * Types of things in a graph.
	 */
	public enum Element {
		NODE, EDGE;
	}
	
	/*
	 * Graph features for nodes, edges, etc - to be calculated upon request.
	 */
	public enum GraphFeature {
		DEGREE(Element.NODE),
		SELF_LOOP(Element.EDGE);
		
		private final Element el;
		private final Feature feature;
		
		private GraphFeature(Element el) {
			this.el=el;
			this.feature=buildFeature(this.name());
		}
		
		private static Feature buildFeature(String type) {
			if (type.equals("DEGREE")) {
				return new Feature("degree", 
						Value.Type.CONTINUOUS, 
						new Value[]{Continuous.makeValue(0), Continuous.makeValue(Double.MAX_VALUE)},
						"graph node degree", "calculated from graph");
			} else if (type.equals(SELF_LOOP)) {
				return new Feature("self_loop", 
						Value.Type.DISCRETE, 
						new Value[]{Discrete.makeValue("true"), Discrete.makeValue("false")},
						"is edge self loop", "calculated from graph");
			}
		
			else return null;
		}
		
		/**
		 * Returns the type of element that the feature is calculated for 
		 */
		public Element element() {
			return this.el;
		}		
		
		/**
		 * Builds the feature fo the requested 
		 * @return
		 */
		public Feature feature() {
			return this.feature;
		}
		
	}	
	
	/**
	 * Gets the nodes in the graph g that are annotated with a specific
	 * feature f.
	 * @param g
	 * @param libe
	 * @param f
	 * @return
	 */
	public static Set<String> request(Graph g, NodeLibrary libe, Feature f) {
		HashSet<String> nodes = new HashSet<String>(libe.get(f));
		if (g != null && g.nodes() != null) {
			nodes.retainAll(g.nodes());
			return nodes;
		} else {
			return new HashSet<String>();
		}
	}
	
	
	/**
	 * Gets the edges in the graph g that are annotated with a specific
	 * feature f.
	 * @param g
	 * @param libe
	 * @param f
	 * @return
	 */
	public static Set<Edge> request(Graph g, EdgeLibrary libe, Feature f) {
		HashSet<Edge> edges = new HashSet<Edge>(libe.get(f));
		edges.retainAll(g.edges());
		return edges;
	}

	/**
	 * Filters a graph down to only nodes that are accepted
	 * by the NodeFilter. Maintains all edges between those nodes.
	 * @param orig	original graph
	 * @param filter	node filter
	 * @return	new graph
	 */
	public static Graph filter(Graph orig, Filter filter, NodeLibrary libe) {
		HashSet<String> keepNodes = new HashSet<String>();
		for (String n : orig.nodes()) {
			if (filter.accept(libe.getValue(n, filter.feature()))) keepNodes.add(n);
		}
		Graph copy = orig.restrict(keepNodes);
		return copy;
	}	
	
	/**
	 * Filters a graph down to only nodes that are accepted
	 * by all NodeFilters. Maintains all edges between those nodes.
	 * @param orig	original graph
	 * @param filter	node filter
	 * @return	new graph
	 */
	public static Graph filterAnd(Graph orig, Collection<Filter> filters, NodeLibrary libe) {
		HashSet<String> keepNodes = new HashSet<String>();
		for (String n : orig.nodes()) {
			boolean keep=true;
			for (Filter filter : filters) 
				if (!filter.accept(libe.getValue(n, filter.feature()))) keep=false;
			if (keep) keepNodes.add(n);
		}
		Graph copy = orig.restrict(keepNodes);
		return copy;
	}	
	
	/**
	 * Filters a graph down to only nodes that are accepted
	 * by at least one NodeFilter. Maintains all edges between those nodes.
	 * @param orig	original graph
	 * @param filter	node filter
	 * @return	new graph
	 */
	public static Graph filterOr(Graph orig, Collection<Filter> filters, NodeLibrary libe) {
		HashSet<String> keepNodes = new HashSet<String>();
		for (String n : orig.nodes()) {
			boolean keep=false;
			for (Filter filter : filters) 
				if (filter.accept(libe.getValue(n, filter.feature()))) keep=true;
			if (keep) keepNodes.add(n);
		}
		Graph copy = orig.restrict(keepNodes);
		return copy;
	}
	
	
	
	/**
	 * Gets the graph-feature-value of a requested node.
	 * Compares the name of "feat" to the set of implemented graph features.
	 * (e.g, node degree.)
	 * @param feat
	 * @param g
	 * @return
	 */
	public static Value getValue(String node, Feature feat, Graph g) {
		GraphFeature gf = GraphFeature.valueOf(feat.name().toUpperCase());
		assert(gf!=null): 
			"Request for unimplemented graph feature " + feat.name();
		switch(gf) {
		case DEGREE: return getDegree(node, g);
		}		
		return null;
	}
	
	protected static Value getDegree(String node, Graph g) {
		Continuous deg = Continuous.makeValue(g.degree(node));
		return deg;
	}		
	
	public static Value getValue(Edge edge, Feature feat, Graph g) {
		GraphFeature gf = GraphFeature.valueOf(feat.name().toUpperCase());
		assert(gf!=null): 
			"Request for unimplemented graph feature " + feat.name();
		switch(gf) {
		case SELF_LOOP: return getIsSelfLoop(edge, g);
		}		
		return null;
	}
	
	protected static Value getIsSelfLoop(Edge edge, Graph g) {
		Value val = Discrete.makeValue(Boolean.toString(edge.isSelfLoop()).toLowerCase());
		return val;
	}
	
	
}
