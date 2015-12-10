package structures;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import utilities.Enums.Sign;
import utilities.StringUtils;

/**
 * This collapser will collapse according to the following rules:
 * 
 * Edges that have the same sign and direction will be merged
 * Edges that have the same direction and no sign will be merged into any signed edges
 * 
 * Undirected, signed edges will be merged into other undirected with same sign.
 * Undirected, unsigned edges will be merged into the signed, undirected edges
 * 
 * So, this collapser will NOT collapse undirected edges when a directed
 * version is available.
 * 
 * Merging entails combining feature information??? 
 * still need to work that part out
 * 
 * @author chasman
 *
 */
public class IfConsistentCollapser implements EdgeCollapser {

	// Are we doing something special with a node or set of nodes?
	protected boolean doForwarding;
	protected HashSet<String> specialDirecting=null;

	public IfConsistentCollapser() {
	}
	
	/**
	 * Set up the nodes for special treatment.
	 * Redirection format:
	 * 	redirect=(node1|node2|...)
	 * 	
	 * @param args
	 */
	public IfConsistentCollapser(String[] args) {
		for (String s : args) {
			// redirect=(node1|node2|...)
			if (s.contains("redirect")) {
				String[] split = s.split("=");
				String boop = split[1].substring(1,split[1].length()-1);
				String[] nodes = boop.split("\\|");
				HashSet<String> nset = new HashSet<String>();
				for (String node : nodes) {
					nset.add(node);
				}
				// set them up
				this.doForwardDirection(nset);
			}
		}
		
	}


	/**
	 * Redirect the edges in a library?
	 * Returns "null" if not specified.
	 */
	public HashMap<Edge, HashSet<Edge>> redirect(EdgeLibrary original) {
		if (!doForwarding) {
			return null;
		}
		HashMap<Edge, HashSet<Edge>> map = new HashMap<Edge, HashSet<Edge>>();
		for (Edge e : original.items()) {
			Edge redir = this.redirect(e);
			if (!map.containsKey(e)) {
				map.put(e, new HashSet<Edge>());
			}
			map.get(e).add(redir);
		}
		return map;
	}
	
	/**
	 * Collapses an EdgeLibrary.
	 * @param original
	 * @return
	 */
	public HashMap<Edge, HashSet<Edge>> collapse(EdgeLibrary original) {

		// draw up a map from the original to the new library:
		// key: original ID
		// value: set of edges in new library that will replace the original
		HashMap<Edge, HashSet<Edge>> map = new HashMap<Edge, HashSet<Edge>>();

		// sort by prefix: a.b.d
		// from there, sort by sign
		HashMap<String, HashMap<Sign, Edge>> collapse = 
			new HashMap<String, HashMap<Sign, Edge>>();		

		for (Edge eo : original.items()) {
			// get all edges between same pair
			HashSet<Edge> connect = original.containsConnection(eo);

			// sort the edges
			for (Edge ec : connect) {
				
				
				String prefix = ec.key.substring(0, ec.key.lastIndexOf("."));
				if (!collapse.containsKey(prefix)) {
					collapse.put(prefix, new HashMap<Sign, Edge>());
				}
				if (!collapse.get(prefix).containsKey(ec.sign())) {
					collapse.get(prefix).put(ec.sign(), ec);
				} 

				assert (collapse.get(prefix).get(ec.sign).equals(ec)) :
					String.format("duplicate edge dir/sign %s %s", ec, collapse.get(prefix).get(ec.sign));
			}
		}

		for (String prefix : collapse.keySet()) {
			String[] sp = prefix.split(".");

			// which signs to do?
			HashMap<Sign, Edge> slot = collapse.get(prefix);

			// if multiple signs and one is "unknown", then move
			// "unknown" edges into the others
			if (slot.size() > 1 && slot.containsKey(Sign.UNKNOWN)) {
				Edge unk = slot.get(Sign.UNKNOWN);
				HashSet<Edge> had = map.put(unk, new HashSet<Edge>());
				assert(had==null) : 
					"Weird duplication??";

				for (Sign s : new Sign[] {Sign.POSITIVE, Sign.NEGATIVE} ) {
					if (slot.containsKey(s)) {
						map.get(unk).add(slot.get(s));
					}
				}					
			}
			// otherwise, just put all of them into their own
			else {
				for (Sign s : slot.keySet()) {
					HashSet<Edge> boop = new HashSet<Edge>();
					boop.add(slot.get(s));
					map.put(slot.get(s), boop);
				}
			}
		}	
		
		return map;
	}

	/**
	 * If e is a PPI involving one of the nodes in "specialDirecting",
	 * we'll map it to a directed edge from the special node to the target.
	 * 
	 * If the edge is actually between TWO nodes from that set, then
	 * return it unchanged.
	 * @param e
	 * @return
	 */
	public Edge redirect(Edge e) {
		Edge retEdge=e;
		if (!e.directed && (specialDirecting.contains(e.i()) || specialDirecting.contains(e.j()))) {
			if (!(specialDirecting.contains(e.i()) && specialDirecting.contains(e.j()))) {				
				String source = specialDirecting.contains(e.i()) ? e.i() : e.j();
				String other =  specialDirecting.contains(e.i()) ? e.j() : e.i();
				retEdge = new Edge(source, other, true, e.sign());
			}
		}
		return retEdge;
	}


	@Override
	public void doForwardDirection(String node) {
		if (this.specialDirecting==null) {
			this.specialDirecting = new HashSet<String>();
		}
		this.specialDirecting.add(node);
		this.doForwarding=true;
	}
	@Override
	public void doForwardDirection(Collection<String> nodes) {
		if (this.specialDirecting==null) {
			this.specialDirecting = new HashSet<String>();
		}
		this.specialDirecting.addAll(nodes);
		this.doForwarding=true;
	}


	@Override
	public boolean doRedirecting() {
		return this.doForwarding;
	}
	
	public String toString() {
		if (this.doRedirecting()) {
		return String.format("IfConsistentCollapser\tredirect=(%s)", 
				StringUtils.sortJoin(this.specialDirecting, "|"));
		} else {
			return "IfConsistentCollapser";
		}
	}

}
