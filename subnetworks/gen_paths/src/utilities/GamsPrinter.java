package utilities;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import structures.BranchyPath;
import structures.Continuous;
import structures.Edge;
import structures.EdgeLibrary;
import structures.Feature;
import structures.Graph;
import structures.Library;
import structures.NodeLibrary;
import structures.Pair;
import structures.PairDirectory;
import structures.Path;
import structures.Path.PathOrder;
import structures.PathManager;
import structures.Subgraph;
import structures.Value;
import utilities.Enums.AddEdgeMode;
import utilities.GraphUtils.Element;

/** 
 *  Given a graph, PathManager, and set of features, prints out the relevant GAMS
 *  sets. Will make special node subsets based on the features defined in the Config file.
 *  We can subclass this in order to add other sets. 
 */

public class GamsPrinter {

	protected Graph graph, bgGraph;
	protected PathManager pm;
	protected NodeLibrary nodeLibe;
	protected EdgeLibrary edgeLibe;
	protected Map<String,Subgraph> subgraphs;
	protected Map<String, PairDirectory> pairDirs;

	protected HashMap<Edge,String> eids;
	protected HashMap<Path,String> pids;

	protected static final int EL_COLS=20;	// 20 single elements per row
	protected static final int TUPLE_COLS=5;	// 5 tuples per row

	// edge feature for complexes
	public static final String ETYPE="etype", IN_CX="in_cx";
	public static final String CX="complex";

	/**
	 * Which edges from a subgraph should be included in the IP input?
	 * @author chasman
	 *
	 */
	protected enum SubgraphMode {
		ALL,	// Include all edges 
		TARGET_IN_PATH, 	// Include edge only if its target is in a path
		SOURCE_IN_PATH; // Include edge only if its source is in a path
	}

	public enum LabelMode {
		QUOTE, // Put quotes around labels, keep all characters
		STRIP;	// Remove punctuation, etc from labels
	}

	/*
	 * By default, strip punctuation.
	 */
	protected LabelMode labelMode=LabelMode.STRIP;

	/**
	 * @param orig	original BG network
	 * @param pm	path manager
	 * @param nodeLibe
	 * @param edgeLibe
	 * @param features
	 */
	public GamsPrinter(Graph orig, PathManager pm, 
			NodeLibrary nodeLibe, EdgeLibrary edgeLibe, 
			Map<String, Subgraph> subgraphs) {
		this.graph=orig;
		this.pm=pm;
		this.nodeLibe=nodeLibe;
		this.edgeLibe=edgeLibe;
		this.subgraphs=subgraphs;


		// add subgraph edges to eids if not present
		HashSet<Edge> allEdges = new HashSet<Edge>(this.graph.edges());
		for (Subgraph s : this.subgraphs.values()) {
			allEdges.addAll(s.edges());
		}

		this.eids=makeUniqueEdgeIds(allEdges);
		this.pids=makeUniquePathIds(this.pm);

	}

	/**
	 * 
	 * @param orig	original BG network
	 * @param pm path manager
	 * @param nodeLibe
	 * @param edgeLibe
	 * @param features
	 */
	public GamsPrinter(Graph orig, PathManager pm, 
			NodeLibrary nodeLibe, EdgeLibrary edgeLibe) {
		this.graph=orig;
		this.pm=pm;
		this.nodeLibe=nodeLibe;
		this.edgeLibe=edgeLibe;
		this.eids=makeUniqueEdgeIds(this.graph.edges());
		this.pids=makeUniquePathIds(this.pm);
		this.subgraphs = new HashMap<String, Subgraph>();
	}	

	/**
	 * 
	 * @param g	path network
	 * @param orig	original BG network
	 * @param pm
	 * @param nodeLibe
	 * @param edgeLibe
	 * @param features
	 */
	public GamsPrinter(Graph g, Graph orig, PathManager pm, 
			NodeLibrary nodeLibe, EdgeLibrary edgeLibe, 
			Map<String, Subgraph> subgraphs, Map<String, PairDirectory> pairDirs) {
		this.graph=g;
		this.pm=pm;
		this.nodeLibe=nodeLibe;
		this.edgeLibe=edgeLibe;

		// add subgraph edges to eids if not present
		HashSet<Edge> allEdges = new HashSet<Edge>(this.graph.edges());
		if (this.subgraphs != null) {
			for (Subgraph s : this.subgraphs.values()) {
				allEdges.addAll(s.edges());
			}
		}

		this.eids=makeUniqueEdgeIds(allEdges);

		this.pids=makeUniquePathIds(this.pm);
		this.subgraphs=subgraphs;
		this.pairDirs = pairDirs;
	}

	/**
	 * Sets label mode to use quotes instead of stripping punctuation.
	 * @param useQuotes
	 */
	public void setLabelMode(LabelMode mode) {
		this.labelMode=mode;
	}

	/**
	 * Print all node sets to stream.
	 * @param stream
	 */
	public void printNodeSets(PrintStream stream) {
		String[] nfeats = new String[this.nodeLibe.features().size()];
		int i=0;
		for (String name : this.nodeLibe.featureNames()) {
			nfeats[i]=name;
			i++;
		}
		this.printNodeSets(stream, nfeats);
	}

	/**
	 * Prints requested node sets to the provided stream.
	 * @param features
	 */
	public void printNodeSets(PrintStream stream, String[] nodeFeatureNames) {
		// first, make the master node set.
		HashSet<String> ids = new HashSet<String>(this.graph.nodes());

		// Make sure we add all nodes from pairs
		if (this.pairDirs != null) {
			for (PairDirectory p : this.pairDirs.values()) {
				ids.addAll(p.getFirsts());
				ids.addAll(p.getSeconds());
			}
		}
		ids = this.gamsifySet(ids);			

		String set = GamsUtils.makeSetList(
				"node", "all nodes", 
				ids, EL_COLS);
		stream.println(set);

		for (String fname : nodeFeatureNames) { //this.nodeLibe.features()) {
			Feature feat = this.nodeLibe.getFeature(fname);
			boolean ok = printFeature(stream, feat);
			assert(ok) : "Problem printing feature from printNodeSets: " + feat.name();
		}			

		// Print complex membership, if complexes present
		Feature cxN = this.nodeLibe.getFeature(CX);
		Feature etype = this.edgeLibe.getFeature(ETYPE);
		if (cxN != null && etype.legal(IN_CX) != null) {
			printComplexSets(stream);
		}

	}

	/**
	 * Print tuples matching nodes with their complexes.
	 * Format:
	 * Set inComplex(node,node) 
	 * / cx.(n0, ... nI) /;
	 * 
	 * @param stream
	 */
	public void printComplexSets(PrintStream stream) {
		// complex membership edges
		Feature etype = this.edgeLibe.getFeature(ETYPE);
		assert(etype != null):"Don't call printComplexSets without having complexes";
		//Set<Edge> inCxEdges = this.edgeLibe.get(etype, etype.legal(IN_CX));


		// complex nodes
		Feature isCxFeat = this.nodeLibe.getFeature(CX);
		Set<String> isCx = this.nodeLibe.get(isCxFeat);

		HashMap<String, HashSet<String>> cxMap = new HashMap<String, HashSet<String>>();

		// for each complex, get members in paths		
		for (String cx : isCx) {
			HashSet<String> members = new HashSet<String>();
			for (Edge e : this.graph.incident(cx)) {
				// keep in-complex edges
				//if (this.edgeLibe.getValue(e, inCxFeat) != null) {
				Value type = this.edgeLibe.getValue(e, etype);
				if (type != null && type.toString().equals(IN_CX)) {
					// add other node 
					for (String o : e.nodes()) {
						members.add(gamsify(o));
					}
				}
			}
			// remove self
			members.remove(cx);
			// add complex if has members
			if (members.size() > 0)	cxMap.put(gamsify(cx), members);			
		}

		// print
		String cxSet = null;
		if (cxMap.size() > 0) {
			cxSet = GamsUtils.makeTupleSet("cxprot(node,node)", "complex membership", cxMap, TUPLE_COLS);
		} else {
			cxSet = GamsUtils.makeEmpty("cxprot(node,node)", "complex membership");
		}		
		stream.println(cxSet);

		// get full membership of candidate complexes using background network edges
		// something wrong here
		Set<Edge> inCx = this.edgeLibe.get(etype, etype.legal(IN_CX));
		HashMap<String,Double> sizes= new HashMap<String,Double>();
		for (Edge e : inCx) {
			for (String n : e.nodes()) {
				// identify complexes that ARE in the candidate network
				if (isCx.contains(n) && this.graph.contains(n) ) {
					if (!sizes.containsKey(n)) sizes.put(n, 0.0);
					sizes.put(n, sizes.get(n)+1.0);
				}
			}
		}

		// parameter for each complex:  size of whole complex
		String cxSizeParam = GamsUtils.makeParameter("complexSize(node)", "size of complete complex", sizes);
		stream.println(cxSizeParam);

		return;
	}

	/**
	 * Prints ALL edge features.
	 * Does not print sign by default.
	 * @param stream
	 */
	public void printEdgeSets(PrintStream stream) {
		this.printEdgeSets(stream, false);
	}

	/**
	 * Prints ALL edge features.
	 * Prints signs if requested.
	 * @param stream
	 */
	public void printEdgeSets(PrintStream stream, boolean printSign) {
		String[] efeats = new String[this.edgeLibe.features().size()];
		int i=0;
		for (String name : this.edgeLibe.featureNames()) {
			efeats[i]=name;
			i++;
		}
		this.printEdgeSets(stream, efeats, printSign);
	}

	/**
	 * Prints requested edge features.
	 * @param features
	 */
	public void printEdgeSets(PrintStream stream, String[] featNames, boolean printSign) {
		// first, make the master edge set.
		HashSet<String> ids = gamsifySet(this.graph.edges());

		String set = GamsUtils.makeSetList(
				"edge", "all edges", 
				ids, EL_COLS);
		stream.println(set);

		// print sets for signs and directions
		HashSet<Edge> signed = new HashSet<Edge>(), activating = new HashSet<Edge>(),
				inhibiting = new HashSet<Edge>();		
		HashSet<Edge> ppi = new HashSet<Edge>();
		for (Edge e : this.graph.edges()) {
			if (!e.isDirected()) ppi.add(e);
			switch(e.sign()) {
			case POSITIVE: signed.add(e); activating.add(e); break;
			case NEGATIVE: signed.add(e); inhibiting.add(e); break;
			default: continue;
			}
		}

		String ppiset = GamsUtils.makeSetList(
				"ppi(edge)", "undirected edges", 
				gamsifySet(ppi), EL_COLS);
		stream.println(ppiset);

		if (printSign) {
			String signset = GamsUtils.makeSetList(
					"signed(edge)", "signed edges", 
					gamsifySet(signed), EL_COLS);
			stream.println(signset);

			String actset = GamsUtils.makeSetList(
					"activating(edge)", "positively-signed edges", 
					gamsifySet(activating), EL_COLS);
			stream.println(actset);

			String inhset = GamsUtils.makeSetList(
					"inhibitory(edge)", "negatively-signed edges", 
					gamsifySet(inhibiting), EL_COLS);
			stream.println(inhset);			
		}

		for (String featName : featNames) {
			Feature feat = this.edgeLibe.getFeature(featName);
			//if (feat==Feature.DEFAULT) continue;

			boolean ok = printFeature(stream, feat);
			assert(ok) : "Problem printing feature from printEdgeSets: " + feat.name();		
		}

		// print edge tuples: eid.n1.n2
		HashSet<String> edgeTups = new HashSet<String>();
		for (Edge e : this.graph.edges()) {
			String eid = this.gamsify(e);
			String nid = this.gamsify(e.i());
			String njd = this.gamsify(e.j());
			edgeTups.add(String.format("%s.%s.%s", eid, nid, njd));
		}
		String enode = GamsUtils.makeSetList("enode(edge,node,node)", "edges with nodes", edgeTups, EL_COLS);
		stream.println(enode);

		//		// mutually exclusive edges
		//		HashSet<String> mutex = getMutuallyExclusiveEdgePairs();
		//		String mxstr = GamsUtils.makeSetList("mutex(edge,edge)", "edges between same nodes", mutex, EL_COLS);
		//		stream.println(mxstr);

	}



	/**
	 * Prints paths! Prints a subset for each label in the Path Manager.
	 * If "hidden" hit set supplied, only print paths that don't begin
	 * with one of the hidden hits.
	 * If no printPathDirs boolean chosen, print the fwd(edge,path) and back(edge,path) 
	 * GAMS sets.
	 * @param stream
	 */
	public void printPathSets(PrintStream stream) {
		printPathSets(stream, true, new HashSet<String>());
	}
	public void printPathSets(PrintStream stream, boolean printPathDirs) {
		printPathSets(stream, printPathDirs, new HashSet<String>());
	}
	public void printPathSets(PrintStream stream, boolean printPathDirs, 
			Set<String> hidden) {
		// First, make the master path set.
		HashSet<Path> allPaths = new HashSet<Path>();

		// either add all paths, or only paths that don't begin
		// with hidden hit.
		if (hidden.size() > 0) {
			for (Path p : this.pm.allPaths()) {
				if (!hidden.contains(p.getNode(0))) {
					allPaths.add(p);
				}
			}
		} else {
			allPaths.addAll(this.pm.allPaths());
		}



		HashSet<String> ids = gamsifySet(allPaths);

		// remove paths that begin with 

		String set = GamsUtils.makeSetList(
				"path", "all paths", 
				ids, EL_COLS);
		stream.println(set);

		// print the path subsets
		for (String label : this.pm.allLabels()) {
			HashSet<Path> lpath = new HashSet<Path>(this.pm.getPathsForLabel(label));
			// keep non-hidden paths
			lpath.retainAll(allPaths);

			HashSet<String> lids = gamsifySet(lpath);

			String lset = GamsUtils.makeSetList(
					String.format("%s(path)", label),
					String.format("paths from pathfinder %s", label), 
					lids, EL_COLS);
			stream.println(lset);
		}		

		// print the "fwd" and "back" subsets, pairing edges and paths
		// do order (e,p) because we'll likely have more p than e.
		if (printPathDirs) {
			// only get path dirs for requested paths
			HashMap<PathOrder, HashMap<String, HashSet<String>>> divided = this.getPathDirs(allPaths);
			String fwdSet = GamsUtils.makeTupleSet("fwd(edge,path)", 
					"undirected edges that proceed forward in paths", 
					divided.get(PathOrder.ABOVE), 
					TUPLE_COLS);
			stream.println(fwdSet);

			String backSet = GamsUtils.makeTupleSet("back(edge,path)", 
					"undirected edges that proceed in reverse in paths", 
					divided.get(PathOrder.BELOW), 
					TUPLE_COLS);
			stream.println(backSet);
		}

		// print nodes per path, edges per path, nodes that start paths, in order (n,p) and (e,p)
		HashMap<String, HashSet<String>> pnodes = new HashMap<String, HashSet<String>>();
		HashMap<String, HashSet<String>> pedges = new HashMap<String, HashSet<String>>();
		HashMap<String, HashSet<String>> pstart = new HashMap<String, HashSet<String>>();

		HashSet<String> npid=new HashSet<String>(), epid=new HashSet<String>();
		for (Path p : allPaths) {
			String pid = this.gamsify(p);
			String start = this.gamsify(p.getNode(0));
			if (!pstart.containsKey(start)) pstart.put(start, new HashSet<String>());
			pstart.get(start).add(pid);

			for (String n : p.nodes()) {
				String nid = this.gamsify(n);
				if (!pnodes.containsKey(nid)) pnodes.put(nid, new HashSet<String>());
				pnodes.get(nid).add(pid);
				npid.add(pid);
			}
			for (Edge e : p.edges()) {				
				String eid = this.gamsify(e);
				if (!pedges.containsKey(eid)) pedges.put(eid, new HashSet<String>());
				pedges.get(eid).add(pid);
				epid.add(pid);
			}	

		}

		String pstartSet = GamsUtils.makeTupleSet("pstart(node,path)", 
				"nodes that start paths", pstart,	TUPLE_COLS);
		stream.println(pstartSet);

		String pnodeSet = GamsUtils.makeTupleSet("pnode(node,path)", 
				"nodes in paths", pnodes,	TUPLE_COLS);
		stream.println(pnodeSet);

		String pedgeSet = GamsUtils.makeTupleSet("pedge(edge,path)", 
				"edges in paths", pedges,	TUPLE_COLS);
		stream.println(pedgeSet);		
	}

	/**
	 * Prints sets related to nonlinear subgraphs: subgraph IDs,
	 * nodes in subgraphs, edges in subgraphs.
	 * @param stream
	 * @param pathman	path manager for edge filter
	 */
	public void printSubgraphSets(PrintStream stream, Map<String, AddEdgeMode> edgeModes) {
		printSubgraphSets(stream, edgeModes, this.pm);
	}

	public void printSubgraphSets(PrintStream stream, Map<String, AddEdgeMode> edgeModes, PathManager pathman) {
		String sgset, edgeStr, nodeStr;
		if (this.subgraphs.size()==0) {
			sgset = GamsUtils.makeEmpty("subgraph", "subgraph IDs");
			edgeStr = GamsUtils.makeEmpty("subedge(subgraph, edge)", "subgraphs and edges");
			nodeStr = GamsUtils.makeEmpty("subnode(subgraph, node)", "subgraphs and nodes");
		}
		else {
			sgset = GamsUtils.makeSetList("subgraph", "subgraph IDs", this.subgraphs.keySet(), EL_COLS);

			// for each subgraph, make node and edge tuples
			HashMap<String, HashSet<String>> nodes =  new HashMap<String, HashSet<String>>(), 
					edges = new HashMap<String, HashSet<String>>();


			for (String subname : subgraphs.keySet()) {
				if (!nodes.containsKey(subname)) {
					nodes.put(subname, new HashSet<String>());
					edges.put(subname, new HashSet<String>());
				}

				Subgraph sub = subgraphs.get(subname);

				// if false, add all nodes/edges to set.
				// if true, add only edges for which at least one node is in
				// the path manager.
				AddEdgeMode mode = edgeModes.get(subname);

				// screen edges
				Collection<Edge> addEdges = PathManager.filterEdges(
						pathman, sub.edges(), mode);
				// add gamsified representation
				for (Edge e : addEdges) {					
					edges.get(subname).add(gamsify(e));
					for (String n : e.nodes()) {
						nodes.get(subname).add(gamsify(n));
					}
				}								

			}
			edgeStr = GamsUtils.makeTupleSet("subedge(subgraph, edge)", "subgraphs and edges", edges, TUPLE_COLS);
			nodeStr = GamsUtils.makeTupleSet("subnode(subgraph, node)", "subgraphs and nodes", nodes, TUPLE_COLS);

		}

		stream.println(sgset);
		stream.println(nodeStr);
		stream.println(edgeStr);
	}

	/**
	 * Prints sets of pairs from Pairdirectories.
	 * @param paths_only	Only print nodes with paths in the pathmanager? 
	 */
	public void printPairSets(PrintStream stream, boolean pathsOnly) {

		for (Entry<String, PairDirectory> pdE : this.pairDirs.entrySet()) {


			// reduce map to pairs in paths and gamsify elements
			HashMap<String, HashSet<String>> map = new HashMap<String, HashSet<String>>();
			for (String first : pdE.getValue().getFirsts()) {
				// in paths?

				if (pathsOnly && !this.pm.contains(first)) continue;
				HashSet<String> tars = new HashSet<String>();
				for (String tar : pdE.getValue().getSeconds(first)) {
					if (pathsOnly && !this.pm.contains(tar)) continue;
					tars.add(gamsify(tar));
				}

				if (tars.size() > 0) {
					map.put(gamsify(first), tars);
				}
			}						

			String name = this.clean(pdE.getKey());
			String set = GamsUtils.makeTupleSet(String.format("%s(node,node)", name), "pairs from " + name, map, TUPLE_COLS);
			stream.println(set);
		}

	}

	/**
	 * Get the order of undirected edges in each path.
	 */
	protected HashMap<PathOrder, HashMap<String, HashSet<String>>> getPathDirs(Set<Path> reqPaths) {
		HashMap<PathOrder, HashMap<String, HashSet<String>>> divided = 
				new HashMap<PathOrder, HashMap<String, HashSet<String>>>();

		for (PathOrder order : new PathOrder[] {PathOrder.ABOVE, PathOrder.BELOW} ) {
			divided.put(order, new HashMap<String, HashSet<String>>());
		}

		for (Path p : reqPaths) {
			// don't do any enforcement for the nonlinear path type

			String pid = this.gamsify(p);
			for (Edge e : p.edges()) {
				String eid = this.gamsify(e);

				if (e.isDirected()) continue;
				PathOrder order = p.order(e.i(), e.j());
				assert(order != PathOrder.NA) : "Shouldn't have undirected paths at this point.";

				HashMap<String, HashSet<String>> subMap = divided.get(order);
				if (!subMap.containsKey(eid)) subMap.put(eid, new HashSet<String>());
				subMap.get(eid).add(pid);
			}
		}		
		return divided;
	}

	/**
	 * Prints GAMS code relevant to a feature.
	 * For discrete or ordinal features, will print a subset of the "node" set
	 * for each feature value.
	 * For node sets/params, we add "N" to the end of the name of the set/param;
	 * for edges, we add E.
	 * 
	 * For continuous features, will print a parameter.
	 * @param feature
	 * @return false if called on non-existent feature.
	 */
	public boolean printFeature(PrintStream stream, Feature feature) {
		Element elType=null;
		Library libe=null;
		String suffix = "!!";

		if (this.nodeLibe.hasFeature(feature)) {
			libe=this.nodeLibe;
			elType=Element.NODE;
			suffix="N";
		} else if (this.edgeLibe.hasFeature(feature)) {
			elType=Element.EDGE;
			libe=this.edgeLibe;
			suffix="E";
		} else {
			return false;
		}

		if (feature.type() == Value.Type.CONTINUOUS) {
			printParameterFeat(elType, suffix, stream, feature);
		} else {
			printDiscreteFeat(elType, suffix, stream, feature);
		}
		return true;
	}

	/**
	 * How to print values of continuous features as parameters on node/edge sets.
	 * @param stream
	 * @param feature
	 */
	protected boolean printParameterFeat(GraphUtils.Element elType, String suffix,
			PrintStream stream, Feature feature) {
		assert(feature.type() == Value.Type.CONTINUOUS) : 
			"Don't call printParameterFeat on non-Continuous features.";


		Library libe = this.nodeLibe;
		Set things = this.graph.nodes();
		if (elType==GraphUtils.Element.EDGE) {
			libe = this.edgeLibe;
			things = this.graph.edges();
		} 
		if (!libe.hasFeature(feature)) {
			return false;
		}

		Set<String> ids = new HashSet<String>();

		HashMap<String,Double> vals = new HashMap<String,Double>();
		for (Object o : things) {
			String id = gamsify(o);

			if (libe.contains(o)) {
				Continuous v = (Continuous) libe.getValue(o, feature);
				if (v != null) {
					ids.add(id);
					vals.put(id, v.getValue());
				}				
			}
		}

		/// clean punctuation, etc from feature name
		String fname = clean(feature.name());
		String all = GamsUtils.makeSetList(
				String.format("%sFeature%s(%s)", fname, suffix, elType.toString().toLowerCase()),
				feature.note(), 
				ids, EL_COLS);
		stream.println(all);

		String param = GamsUtils.makeParameter(
				String.format("%sParam%s(%s)", fname, suffix, elType.toString().toLowerCase()), 
				feature.note(), vals);

		stream.println(param);
		return true;		
	}

	/**
	 * How to print values of discrete features as subsets of node/edge sets.
	 * @param stream
	 * @param feature
	 */
	protected boolean printDiscreteFeat(GraphUtils.Element elType, String suffix,
			PrintStream stream, Feature feature) {
		assert(feature.type() != Value.Type.CONTINUOUS) : 
			"Don't call printDiscreteFeat on Continuous features.";


		Library libe = this.nodeLibe;
		Set graphThings = this.graph.nodes();
		if (elType==GraphUtils.Element.EDGE) {
			libe = this.edgeLibe;
			graphThings = this.graph.edges();
		} 

		Set allThings = libe.get(feature);
		if (allThings==null) return false;
		allThings.retainAll(graphThings);

		HashSet<String> ids = new HashSet<String>();
		for (Object o : allThings) {
			String id = gamsify(o);
			ids.add(id);
		}

		// remove non-alphanumeric characters from feature name
		String featName = clean(feature.name());
		String all = GamsUtils.makeSetList(
				String.format("%sFeature%s(%s)", featName, suffix, elType.toString().toLowerCase()),
				feature.note(), 
				ids, EL_COLS);
		stream.println(all);

		Value[] vals = feature.values();
		for (Value v : vals) {
			Set subThings = libe.get(feature, v);
			HashSet<String> subIds = new HashSet<String>();
			for (Object o : subThings) {
				if (!graphThings.contains(o)) continue;

				String id = gamsify(o);
				subIds.add(id);
			}

			// remove non-alphanumeric chars
			String valName = clean(v.toString());
			String set = GamsUtils.makeSetList(
					String.format("%sValue%s(%s)", valName, suffix, elType.toString().toLowerCase()),
					String.format("%s=%s", feature.name(), v.toString()), 
					subIds, EL_COLS);
			stream.println(set);
		}		

		return true;		
	}

	/**
	 * Cleans non alphanumeric characters from string
	 * @param s
	 * @return
	 */
	public String clean(String s) {
		return s.replaceAll("[^A-Za-z0-9]", ""); 
	}

	/**
	 * Produces a GAMS ID for an input object.
	 * We assume that the object is a graph element: node, edge, path, cycle, etc.
	 * @param o
	 * @return
	 */
	public String gamsify(Object o) {
		String orig=o.toString();

		// edges - get unique ID
		if (o instanceof Edge) {
			orig = eids.get((Edge) o);
		} else if (o instanceof Path) {
			orig = pids.get((Path) o);
		}

		// null item wasn't gamsified previously?
		assert(orig != null): String.format("Problem: item %s wasn't gamsified? Returned null.", o.toString());

		// strip out extra punctuation if mode
		String update=null;
		switch(labelMode) {
		case STRIP: update = orig.replaceAll("[^A-Za-z0-9]", ""); break; 
		case QUOTE: update = String.format("'%s'", orig); break;
		}

		assert(update.length() > 0): 
			"Lost name entirely by removing non-alphanumeric chars: " + orig;
		return update;
	}

	/**
	 * Each pair of nodes can only have at most one edge between them.
	 * This retrieves the edges that occur between pairs of nodes in the graph
	 * and makes pairs out of each pair of them.
	 * 
	 * In each pair, edges are in alphabetical order by GAMS ID.
	 * @return
	 */
	protected HashSet<String> getMutuallyExclusiveEdgePairs() {
		HashSet<String> tuples = new HashSet<String>();
		for (String nodeA : this.graph.nodes()) {
			for (String nodeB : this.graph.neighbors(nodeA)) {
				Set<Edge> inc = this.graph.adjacent(nodeA, nodeB);
				if (inc.size() > 1 ) {
					Set<Pair<Edge,Edge>> pairs = GenUtils.allPairs(inc);
					for (Pair<Edge,Edge> p : pairs) {
						String a = gamsify(p.first()), b = gamsify(p.second());
						if (a.compareTo(b) > 0) {
							String temp = a;
							a=b;
							b=temp;
						}
						tuples.add(String.format("%s.%s", a, b));
					}
				}
			}
		}

		return tuples;
	}

	/**
	 * Converts a set (or unordered collection) of items to their GAMSified IDs.
	 * 
	 * @param <T>
	 * @param items
	 * @return
	 */
	public <T> HashSet<String> gamsifySet(Collection<T> items) {
		HashSet<String> ids = new HashSet<String>();
		for (T p : items) {
			ids.add(gamsify(p));
		}
		return ids;
	}

	/**
	 * Converts a collection (ordered or not) of items to their GAMSified IDs. 
	 * Maintains order, if there is one.
	 * @param <T>
	 * @param items
	 * @return
	 */
	public <T> ArrayList<String> gamsifyList(Collection<T> items) {
		ArrayList<String> ids = new ArrayList<String>();
		for (T p : items) {
			ids.add(gamsify(p));
		}
		return ids;
	}

	/**
	 * Makes unique IDs for the edges in a graph.
	 * 
	 * @return
	 */
	protected static HashMap<Edge, String> makeUniqueEdgeIds(Set<Edge> edges) {
		HashMap<Edge, String> map = new HashMap<Edge, String>();
		int i=0;
		for (Edge e : edges) {
			String id = String.format("edge%d", i);
			i++;
			map.put(e,id);
		}
		return map;
	}

	/**
	 * Retrieves the mapping from edge to unique ID.
	 * @return
	 */
	public Map<Edge, String> getEdgeIDs() {
		return Collections.unmodifiableMap(this.eids);
	}


	/**
	 * Makes unique IDs for the paths in a PathManager
	 * 
	 * @return
	 */
	protected static HashMap<Path, String> makeUniquePathIds(PathManager pm) {
		HashMap<Path, String> map = new HashMap<Path, String>();
		int i=0;
		for (Path p : pm.allPaths()) {
			String id = String.format("p%d", i);
			i++;
			map.put(p,id);
		}
		return map;
	}

	/**
	 * Prints out a string representation of the nodes.
	 * Format:
	 * A|B|C for regular paths
	 * or
	 * A|B|C|[T1,TN] for branchy paths
	 * where [ ] holds the terminal nodes.
	 * 
	 * 
	 * @return
	 */
	public String nodeString(Path p) {
		StringBuilder sb = new StringBuilder();

		if (p instanceof BranchyPath) {
			BranchyPath bp = (BranchyPath) p;
			sb.append(StringUtils.join(gamsifyList(bp.bodyPath().nodes()), "|"));
			if (bp.termini().size() > 0) {
				sb.append(String.format("|[%s]", StringUtils.join(gamsifySet(bp.termini()), ",")));
			}
		}
		else {
			sb.append(StringUtils.join(gamsifyList(p.nodes()), "|"));
		}


		return sb.toString();
	}

	/**
	 * Prints out a string representation of the edges.
	 * Format:
	 * E1|E2|E3|[E4,E5]
	 * where [ ] holds the terminal edges.
	 * @return
	 */
	public String edgeString(Path p) {
		StringBuilder sb = new StringBuilder();

		if (p instanceof BranchyPath) {
			BranchyPath bp = (BranchyPath) p;
			sb.append(StringUtils.join(gamsifyList(bp.bodyPath().edges()), "|"));
			if (bp.termini().size() > 0) {
				sb.append(String.format("|[%s]", StringUtils.join(gamsifyList(bp.terminalEdges()), ",")));
			}
		}
		else {
			sb.append(StringUtils.join(gamsifyList(p.edges()), "|"));
		}		

		return sb.toString();
	}



}
