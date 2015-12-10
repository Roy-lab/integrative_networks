package structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import pathfinders.PathFinder;
import structures.EdgeCollapser.Collapser;
import structures.EdgeLibrary.IncompatibleException;
import utilities.Enums.AddEdgeMode;
import utilities.GamsPrinter.LabelMode;
import utilities.GenUtils;
import utilities.GraphUtils;
import utilities.GraphUtils.GraphFeature;
import exceptions.DuplicateException;
import exceptions.IncomparableException;
import exceptions.InvalidValueException;
import filters.EdgeFilterManager;
import filters.Filter;
import filters.FilterManager;
import filters.FilterManager.FilterItemMode;
import filters.GraphNodeFilterManager;
import filters.NodeFilterManager;

/**
 * Contains the configuration for the run and helps us read stuff.
 * - List of relevant features
 * - List of node and edge filters
 * - Pathfinding info
 * @author chasman
 *
 */
public final class Configuration {

	public static final String NODE_FEATURE="NFEATURE", EDGE_FEATURE="EFEATURE",
	EDGE_FILTER="EFILTER", NODE_FILTER="NFILTER", EDGE_LIBRARY="EDGE_LIBRARY",
	NFILTER_MODE="NFILTERMODE", EFILTER_MODE="EFILTERMODE",
	NODE_FILTER_MAN="NFILTERMANAGER", EDGE_FILTER_MAN="EFILTERMANAGER",
	NAME="NAME", PATHFINDER="PATHFINDER",EDGE_OVERRIDE="EDGE_OVERRIDE";

	public static final String GRAPH_FILTER="GFILTER", G_FILTER_MAN="GFILTERMANAGER",
	FILTER_GRAPH="FILTER_GRAPH", GAMS_FILE="GAMS_FILE", INDIRECTORY="INDIRECTORY", PAIRS="ST_PAIRS",
	COLLAPSER="COLLAPSER", OUTPUT="OUT_PREFIX", SUBGRAPH="SUBGRAPH", GAMS_REQ_CAND="GAMS_REQ_CAND",
	GAMS_EFEATS="GAMS_EFEATS", GAMS_NFEATS="GAMS_NFEATS", GAMS_LABEL_MODE="GAMS_LABEL_MODE",
	FILTER_PATHS_BY_EDGES="FILTER_PATHS_BY_EDGES";
	
	public static final String NODE_MAP="NODE_MAP";
	
	public static final String SCORED_PAIRDIR="SCORED_PAIRS";
	
	public static final String HIDE_HIT_DIR="HIDE_HIT_DIR";

	public static final String AND="and", OR="or"; 

	public static final String VAL_DELIM="\\|";
	public static final String FIELD_DELIM="\t";
	public static final String COMMENT="#";	

	protected HashMap<String, NodeFilterManager> nodeFilterMans;
	protected HashMap<String, GraphNodeFilterManager> graphFilterMans;
	protected HashMap<String, EdgeFilterManager> edgeFilterMans;

	protected HashMap<String, Filter> nodeFilters;
	protected HashMap<String, Filter> edgeFilters;
	protected HashMap<String, Filter> graphFilters;

	// Apply all of these filter managers to the graph.
	// Apply in order listed on the "FILTER_GRAPH" line.
	protected ArrayList<FilterManager> applyToGraph;

	// By default, don't run any filters unless specified.
	//protected FilterItemMode nfilterMode = FilterItemMode.NONE;
	//protected FilterItemMode efilterMode = FilterItemMode.NONE;
	
	/*
	 * We can specify an edge feature for overriding other edges 
	 * in the background network between those nodes.
	 * Doesn't matter what the value of the feature is.
	 */
	protected String edgeOverrideFeature=null;

	protected NodeLibrary nodeLibe;
	protected EdgeLibrary edgeLibe;

	// Which graph features are used in this configuration?
	protected HashSet<String> graphFeats;


	// Are we filtering based on indirect relationships?
	protected HashMap<String, PairDirectory> pairDirs;
	
	// Scored, ordered pairwise relationships
	protected HashMap<String, ScoredPairDirectory> scoredPairs;

	protected ArrayList<PathFinder> pathFinders;

	protected HashMap<String, Subgraph> subgraphs;

	// When printing to GAMS sets, specify which subgraph edges should be added
	protected HashMap<String, AddEdgeMode> subgraphAddMode;

	// edge library collapse mode
	protected EdgeCollapser collapser;

	protected String gamsFn="System.out";
	protected String outPrefix="output";
	protected String hideHitDir=null;
		
	// which edge and node features to print to GAMS file
	protected String[] gamsEFeats=null, gamsNFeats=null;
	
	// gams label mode?
	protected LabelMode gamsLabelMode = LabelMode.STRIP;


	protected Configuration() {
		nodeLibe = new NodeLibrary();
		edgeLibe = new EdgeLibrary();

		nodeFilters = new HashMap<String, Filter>();
		edgeFilters = new HashMap<String, Filter>();
		this.graphFilters = new HashMap<String, Filter>();

		nodeFilterMans = new HashMap<String, NodeFilterManager>();
		graphFilterMans = new HashMap<String, GraphNodeFilterManager>();
		edgeFilterMans = new  HashMap<String, EdgeFilterManager>();

		this.applyToGraph = new ArrayList<FilterManager>();

		pairDirs = new HashMap<String, PairDirectory>();

		pathFinders = new ArrayList<PathFinder>();

		subgraphs = new HashMap<String, Subgraph>();
		subgraphAddMode = new HashMap<String, AddEdgeMode>();
	}

	/**
	 * Returns the graph built from the Config's edge libraries,
	 * with all FILTER_GRAPH-requested filters run. 
	 * @return
	 */
	public Graph buildGraph() throws DuplicateException {
		Graph g = null;
		if (this.edgeOverrideFeature != null) {
			g=Graph.createFromEdgeLibrary(this.edgeLibrary(), this.getEdgeOverrideFeature());
		} else {
			g= Graph.createFromEdgeLibrary(this.edgeLibrary());
		}
		
		for (FilterManager fm : this.applyToGraph) {
			g = fm.filter(g);
		}

		return g;
	}

	public NodeLibrary nodeLibrary() {
		return this.nodeLibe;
	}

	public EdgeLibrary edgeLibrary() {
		return this.edgeLibe;
	}

	public Map<String, Filter> nodeFilters() {
		return Collections.unmodifiableMap(this.nodeFilters);
	}

	public ArrayList<PathFinder> pathFinders() {
		return this.pathFinders;
	}

	public String getGamsFileName() {
		return this.gamsFn;
	}
	protected void setGamsFile(String gamsFn) {
		this.gamsFn=gamsFn;
	}
	
	public LabelMode getGamsLabelMode() {
		return this.gamsLabelMode;
	}
	
	protected void setGamsLabelMode(LabelMode mode) {
		this.gamsLabelMode = mode;
	}
	
	public String[] getGamsEdgeFeatureNames() {
		return this.gamsEFeats;
	}
	protected void setGamsEdgeFeatureNames(String[] feats) {
		this.gamsEFeats=feats;
	}
	public String[] getGamsNodeFeatureNames() {
		return this.gamsNFeats;
	}
	protected void setGamsNodeFeatureNames(String[] feats) {
		this.gamsNFeats=feats;
	}
	
	protected String getEdgeOverrideFeature() {
		return this.edgeOverrideFeature;
	}
	
	protected void setEdgeOverrideFeature(String name) {		
		this.edgeOverrideFeature=name;
	}
	
	
	public String getOutputPrefix() {
		return this.outPrefix;
	}
	protected void setOutputPrefix(String pref) {
		this.outPrefix=pref;
	}
	
	public String getHiddenHitDirectory() {
		return this.hideHitDir;
	}
	protected void setHiddenHitDirectory(String fn) {
		this.hideHitDir=fn;
	}


	/**
	 * Gets a node filter by name, or returns null.
	 * @param name
	 * @return
	 */
	public Filter getNodeFilter(String name) {
		return this.nodeFilters.get(name);
	}

	public NodeFilterManager getNodeFilterManager(String name) {
		return nodeFilterMans.get(name);
	}

	public EdgeFilterManager getEdgeFilterManager(String name) {
		return edgeFilterMans.get(name);
	}

	public GraphNodeFilterManager getGraphFilterManager(String name) {
		return graphFilterMans.get(name);
	}

	public Subgraph getSubgraph(String name) {
		return this.subgraphs.get(name);
	}

	public Map<String, Subgraph> subgraphs() {
		return Collections.unmodifiableMap(this.subgraphs);
	}

	public Map<String, AddEdgeMode> subgraphAddModes() {
		return Collections.unmodifiableMap(this.subgraphAddMode);
	}

	public Map<String, GraphNodeFilterManager> graphFilterManagers() {
		return Collections.unmodifiableMap(this.graphFilterMans);
	}

	public Map<String, NodeFilterManager> nodeFilterManagers() {
		return Collections.unmodifiableMap(this.nodeFilterMans);
	}

	public Map<String, Filter> edgeFilters() {
		return Collections.unmodifiableMap(this.edgeFilters);
	}

	public Filter getEdgeFilter(String name) {
		return this.edgeFilters.get(name);
	}

	public Map<String, Filter> graphFilters() {
		return Collections.unmodifiableMap(this.graphFilters);
	}

	public Filter getGraphFilter(String name) {
		return this.graphFilters.get(name);
	}

	public Map<String, PairDirectory> pairDirectories() {
		return Collections.unmodifiableMap(this.pairDirs);
	}

	public PairDirectory getPairDirectory(String name) {
		return this.pairDirs.get(name);
	}


	/**
	 * Provides a printable summary of the contents of this
	 * configuration.
	 * @return
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Configuration:\n");

		// node libe
		sb.append(String.format("%s\n", nodeLibe.toString()));

		// edge libe
		sb.append(String.format("%s", edgeLibe.toString()));

		return sb.toString();		
	}


	protected boolean addNodeFeature(Feature f, Map<String, Value> vals) {
		return this.nodeLibe.addValues(f, vals);
	}

	/**
	 * Read the config file.
	 * Skip comments
	 * @param configFile
	 * @return
	 */
	public static Configuration readConfigFile(String configFile) 
	throws DuplicateException, IOException, 
	InvalidValueException, 
	IncomparableException, 
	IncompatibleException {
		Scanner s = null;
		try {
			s = new Scanner(new File(configFile));
		} catch (FileNotFoundException fnfe) {
			throw new FileNotFoundException(
					String.format("Unable to find config file '%s'", configFile));
		}

		Configuration config=new Configuration();

		while (s.hasNext()) {
			String line = s.nextLine().trim();
			if (line.startsWith(COMMENT) || line.length() == 0) {
				continue;
			} 
			String[] sp = line.split(FIELD_DELIM);

			// Contents of field 0 tells us what to do with this line.
			if (sp[0].equals(NODE_FEATURE) && sp.length > 2) {				
				Pair<Feature, Map<String, Value>> pair = readNodeFeature(sp);
				config.addNodeFeature(pair.first(), pair.second());
			} 
			else if (sp.length == 2 &&
					(sp[0].equals(NODE_FEATURE) || sp[0].equals(EDGE_FEATURE))) {
				Feature feat = Feature.readFeatureDeclaration(sp[1], "read from config", configFile);
				try {
					GraphFeature gf = GraphFeature.valueOf(feat.name());
					throw new InvalidValueException(String.format("Declared feature name '%s' " +
							"is reserved for graphs.", feat.name()));
				} catch (IllegalArgumentException iae) {
					// OK
				}
				if (sp[0].equals(NODE_FEATURE)) {
					config.nodeLibe.addFeature(feat);
				} else {
					config.edgeLibe.addFeature(feat);
				}
			}

			else if (sp[0].equals(EDGE_LIBRARY)) {
				EdgeLibrary newEl = readEdgeFile(config, sp);
				config.edgeLibe.addAll(newEl);	
				// If collapser declared, collapse it.
				if (config.collapser != null) {
					config.edgeLibe = config.edgeLibe.collapse(config.collapser);
				}
			} 

			else if (sp[0].equals(NODE_FILTER)) {
				Feature f = config.nodeLibe.getFeature(sp[3]);
				if (f==null) {
					throw new InvalidValueException(
							String.format("Node feature '%s' not declared " +
									"(at least not before before Filter '%s')", sp[3], sp[1]));
				}
				Filter filter = Filter.makeFilter(sp, f);
				if (filter==null) {
					throw new InvalidValueException(
							String.format("Filter type '%s' not yet implemented", sp[1]));
				}
				config.addNodeFilter(filter);
			} 

			else if (sp[0].equals(EDGE_FILTER)) {
				Feature f = config.edgeLibe.getFeature(sp[3]);
				if (f==null) {
					throw new InvalidValueException(
							String.format("Edge feature '%s' not declared " +
									"(at least not before before Filter '%s')", sp[3], sp[1]));
				}
				Filter filter = Filter.makeFilter(sp, f);
				config.addEdgeFilter(filter);
			} 

			else if (sp[0].equals(GRAPH_FILTER)) {
				// see if we can make a GraphFilter out of it
				Filter f = makeGraphFilter(sp);
				config.addGraphFilter(f);					
			} 
			
			else if (sp[0].equals(NODE_MAP)) {				
				config.mapNodes(sp);
			}

			else if (sp[0].equals(NODE_FILTER_MAN)) {
				NodeFilterManager nFilterMan = NodeFilterManager.readFilterManager(sp, config);
				config.addFilterManager(nFilterMan);
			}

			else if (sp[0].equals(EDGE_FILTER_MAN)) {
				EdgeFilterManager eFilterMan = EdgeFilterManager.readFilterManager(sp, config);
				config.addFilterManager(eFilterMan);
			} 

			else if (sp[0].equals(G_FILTER_MAN)) {
				GraphNodeFilterManager gfm = GraphNodeFilterManager.readFilterManager(sp, config);
				config.addFilterManager(gfm);
			}
			else if (sp[0].equals(EDGE_OVERRIDE)) {
				String name=sp[1];
				if (config.edgeLibe==null || !config.edgeLibe.hasFeature(name)) {					
					throw new InvalidValueException(
							String.format("Please declare feature %s before assigning it OVERRIDE status.", name));
				}	
				String hasOver=config.getEdgeOverrideFeature();
				if (hasOver != null) {
					throw new InvalidValueException(
							String.format("You can only declare one edge override feature. You declared %s before %s.", name, hasOver));
				}
				config.setEdgeOverrideFeature(name);				
			}

			else if (sp[0].equals(PATHFINDER)) {
				PathFinder pf = PathFinder.readPathFinder(sp, config);
				config.addPathFinder(pf);
			}
			else if (sp[0].equals(FILTER_PATHS_BY_EDGES)) {
				// sp 1: pathfinder
				// sp 2: edge filter manager
				EdgeFilterManager efm = config.getEdgeFilterManager(sp[2]);
				if (efm == null) {
					throw new InvalidValueException(
							String.format("Trying to add EdgeFilterManager %s" +
									" to PathFinder %s: EdgeFilterManager not yet declared.", sp[2], sp[1]));
				}
				
				boolean found=false;
				for (PathFinder p : config.pathFinders ) {
					if (p.name().equals(sp[1])) {
						p.addEdgeFilterManager(efm);
						found=true;
						break;
					}
				}
					
				if (!found) {
					throw new InvalidValueException(
								String.format("Trying to add EdgeFilterManager %s" +
										" to PathFinder %s: PathFinder not yet declared.", sp[2], sp[1]));
				}
			}
			
			else if (sp[0].equals(FILTER_GRAPH)) {
				config.setGraphFiltering(sp);
			} 

			else if (sp[0].equals(GAMS_FILE)) {
				config.setGamsFile(sp[1]);
			}
			else if (sp[0].equals(GAMS_LABEL_MODE)) {
				config.setGamsLabelMode(LabelMode.valueOf(sp[1].toUpperCase()));
			}
			else if (sp[0].equals(OUTPUT)) {
				config.setOutputPrefix(sp[1]);
			}
			else if (sp[0].equals(HIDE_HIT_DIR)) {
				if ((new File(sp[1]).isDirectory())) {
					config.setHiddenHitDirectory(sp[1]);
				} else {
					throw new InvalidValueException("Can't find directory of held-aside hit files: " + sp[1]);
				}
			}
			else if (sp[0].equals(INDIRECTORY) || sp[0].equals(PAIRS)) {
				PairDirectory indie = null;
				if (sp[0].equals(INDIRECTORY)) {
					indie = PairDirectory.readIndirectory(sp[2], sp[1]);
				} else if (sp[0].equals(PAIRS)) {
					indie = readPairDir(sp);
				}
				PairDirectory overwrite = config.addPairDirectory(sp[1], indie);
				if (overwrite != null) {
					throw new DuplicateException("Found duplicate PairDirectory name: " + sp[1]);
				}
			} 
			// Edge collapser for libraries?
			else if (sp[0].equals(COLLAPSER)) {
				Collapser c = Collapser.fromName(sp[1]);							
				if (c == null) throw new InvalidValueException(
						String.format("Requested EdgeCollapser %s doesn't exist yet.", sp[1]));
				// peel off the first two args
				String[] args = new String[sp.length-2];
				System.arraycopy(sp, 2, args, 0, args.length);
				config.collapser=c.make(args);						
			}
			// Build a subgraph from edge filter managers and/or node filter
			// managers.
			else if (sp[0].equals(SUBGRAPH)) {
				System.out.println("Constructing subgraph...");
				Subgraph sub = makeSubgraph(sp, config);

				// how to add subgraph?
				// all, or only edges with source/target in candidate paths?
				AddEdgeMode addMode = AddEdgeMode.ALL;
				if (sp.length > 3) {
					try {
						addMode = AddEdgeMode.valueOf(sp[3]);
					} catch (IllegalArgumentException iae) {
						throw new InvalidValueException("Invalid edge-addition mode:" + sp[3]);
					}
				}

				if (config.subgraphs.containsKey(sub.name())) {
					throw new DuplicateException(
							String.format("Subgraph with name %s has already been declared.", sub.name()));
				}
				config.addSubgraph(sub.name(), sub);
				config.setSubgraphAddMode(sub.name(), addMode);
			}
			// Which edge features to print to GAMS file? | delimited list.
			else if (sp[0].equals(GAMS_EFEATS)) {
				String[] feats = sp[1].split(VAL_DELIM);
				// check each one
				for (String f : feats) {
					if (!config.edgeLibe.featureNames().contains(f)) {
						throw new InvalidValueException("Requested invalid edge feature for GAMS output:" + f);
					}
				}
				config.setGamsEdgeFeatureNames(feats);
			}
			// Which node features to print to GAMS file? | delimited list.
			else if (sp[0].equals(GAMS_NFEATS)) {
				String[] feats = sp[1].split(VAL_DELIM);
				// check each one
				for (String f : feats) {
					if (!config.nodeLibe.featureNames().contains(f)) {
						throw new InvalidValueException("Requested invalid node feature for GAMS output:" + f);
					}
				}
				config.setGamsNodeFeatureNames(feats);
			}

			else {
				System.err.println("Config line type not implemented:" + sp[0]);
			}
		}

		return config;		
	}

	/**
	 * Makes a Filter based on a graph feature.
	 * Config line looks like this: 
	 * 	GFILTER	name	OrderedFilter	degree	1000	lesser
	 * Expect
	 * @param line
	 * @return
	 */
	protected static Filter makeGraphFilter(String[] line)
	throws InvalidValueException, IncomparableException {
		assert(line[0].equals("GFILTER")):"You shouldn't call " +
		"makeGraphFilter on a non-graphfilter.\n" + Arrays.toString(line);

		GraphFeature graphFeat = null;
		try {		
			graphFeat = GraphFeature.valueOf(line[3].toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new InvalidValueException(
					String.format("Graph feature %s not implemented.", line[3]));
		}

		Feature f = graphFeat.feature();

		Filter filter = Filter.makeFilter(line, f);
		return filter;
	}

	/**
	 * Constructs a subgraph object from the info in the line.
	 * For now, filter managers are or-ed together.
	 * (Nodes in edges don't need to pass node filter managers.)
	 * @param line
	 * @return
	 */
	protected static Subgraph makeSubgraph(String[] line, Configuration config) 
	throws InvalidValueException {
		String name = line[1];

		// line will contain node/edge filter managers
		// listed as: edge=e_m|node=node_m ... etc
		// If one more field and contains "GAMS_REQ_CAND", then give it a "true"
		// flag for only GAMS-ifying those subgraph edges that contain at least one
		// candidate node.

		HashSet<Edge> edges = new HashSet<Edge>();
		HashSet<String> nodes = new HashSet<String>();

		String[] mans = line[2].split(VAL_DELIM);
		for (String man : mans) {
			String[] msp = man.split("=");
			if (msp[0].equalsIgnoreCase("edges")) {
				EdgeFilterManager efm = config.getEdgeFilterManager(msp[1]);
				if (efm==null) {
					throw new InvalidValueException(String.format(
							"Subgraph %s declaration: Unable to find EdgeFilterManager %s.", name, msp[1]));
				}
				Collection<Edge> accepted = efm.apply(efm.library().items());
				edges.addAll(accepted);
			} 
			else if (msp[0].equalsIgnoreCase("nodes")) {
				NodeFilterManager nfm = config.getNodeFilterManager(msp[1]);
				if (nfm==null) {
					throw new InvalidValueException(String.format(
							"Subgraph %s declaration: Unable to find NodeFilterManager %s.", name, msp[1]));
				}
				Collection<String> accepted = nfm.apply(nfm.library().items());
				nodes.addAll(accepted);
			}
		}	

		Subgraph sub = new Subgraph(name, nodes, edges);
		return sub;
	}

	/**
	 * Sets up the filter managers that will be applied to the
	 * entire graph.
	 * Example:
	 * FILTER_GRAPH	fmA|fmB
	 * @param sp
	 */
	protected void setGraphFiltering(String[] sp) throws InvalidValueException {
		String[] manNames = sp[1].split(VAL_DELIM);
		for (String name : manNames) {
			if (this.nodeFilterMans.containsKey(name)) {
				this.applyToGraph.add(nodeFilterMans.get(name));
			} else if (this.graphFilterMans.containsKey(name)) {
				this.applyToGraph.add(graphFilterMans.get(name));
			} else if (this.edgeFilterMans.containsKey(name)) {
				this.applyToGraph.add(edgeFilterMans.get(name));
			}
			else {
				throw new InvalidValueException("Couldn't find requested FilterManager " + name);
			}
		}
	}


	protected void addFilterManager(FilterManager man) {
		if (man instanceof NodeFilterManager) 
			this.nodeFilterMans.put(man.name(), (NodeFilterManager) man);
		else if (man instanceof GraphNodeFilterManager) 
			this.graphFilterMans.put(man.name(), (GraphNodeFilterManager) man);
		else if (man instanceof EdgeFilterManager) 
			this.edgeFilterMans.put(man.name(), (EdgeFilterManager) man);
		else {
			assert(false) : "Tried to add illegal filter manager to config";
		}
		return;
	}

	protected void addNodeFilter(Filter filt) {
		this.nodeFilters.put(filt.name(), filt);
	}
	protected void addEdgeFilter(Filter filt) {
		this.edgeFilters.put(filt.name(), filt);
	}

	protected void addGraphFilter(Filter filt) {
		this.graphFilters.put(filt.name(), filt);
	}


	protected void addPathFinder(PathFinder pf) {
		this.pathFinders.add(pf);
	}
	
	protected PairDirectory addPairDirectory(String name, PairDirectory dir) {
		return this.pairDirs.put(name, dir);
	}
	
	protected ScoredPairDirectory addScoredPairDirectory(String name, ScoredPairDirectory pdir) {
		return this.scoredPairs.put(name, pdir);
	}

	protected Subgraph addSubgraph(String name, Subgraph subgraph) {
		return this.subgraphs.put(name, subgraph);
	}
	protected AddEdgeMode setSubgraphAddMode(String name, AddEdgeMode val) {
		return this.subgraphAddMode.put(name, val);
	}

	/**
	 * Reads pair directory from config line:
	 * ST_PAIRS	name	filename	positive=POSTEXT,negative=NEGTEXT,format=%s_D
	 * OK to leave out positive, negative, or both. (will default to "unsigned")
	 * OK to leave out format.
	 * @param sp
	 * @return
	 */
	protected static PairDirectory readPairDir(String[] sp)
	throws InvalidValueException, IOException, DuplicateException {
		String name = sp[1];
		String filename = sp[2];
		String format="%s";
		String posText=null;
		String negText=null;
		boolean readSigns=false;

		// just filename? return.
		if (sp.length == 2) {
			return PairDirectory.readOrderedPairs(filename, name);
		}

		// may not specify signs, etc
		if (sp.length > 3) {
			String[] args = sp[3].split(",");
			for (String s : args) {
				String[] again = s.split("=");
				if (again.length!=2) {
					throw new InvalidValueException(String.format(
							"Invalid argument to PairDirectory %s: %s",name, s)); 
				}
				if (again[0].equalsIgnoreCase("positive")) {
					posText = again[1];
					readSigns=true;
				} else if (again[0].equalsIgnoreCase("negative")) {
					negText = again[1];
					readSigns=true;
				} else if (again[0].equalsIgnoreCase("format")) {
					format = again[1];
				}
			}
		}
		return PairDirectory.readOrderedPairs(filename,name,  format, 
				readSigns, posText, negText);	


	}
	
	/**
	 * Parses ScoredPairDirectory line. Builds from two existing PairDirectories using the
	 * specified scoring mechanisms.
	 * 
	 * SCORED_PAIR_DIRECTORY	name	PairDir1	PairDir2	jaccard
	 * 
	 * @param sp
	 * @return
	 */
	protected ScoredPairDirectory readScoredPairLine(String[] sp) {
		throw new RuntimeException("NOT IMPLEMENTED YET");
		// START HERE
	}
	
	/**
	 * Reads in a node-mapping file and replaces nodes in edge library and node library.
	 * Use example: replacing DOT6 and TOD6 with DOT6TOD6.
	 * @param sp
	 */
	protected void mapNodes(String[] sp) throws InvalidValueException, IOException, DuplicateException {
		if (sp.length != 2) {
			throw new InvalidValueException("NODE_MAP line must specify a filename in field 1.");
		}
		
		String fn=sp[1];
		HashMap<String,String> mapper=GenUtils.readMap(fn, FIELD_DELIM);
		for (Entry<String,String> entry : mapper.entrySet()) {
			// replace in node library. print out if original not present.
			if (!this.nodeLibe.contains(entry.getKey())) {
				System.out.format("Node library does not contain item to be replaced: %s.\n", entry.getKey());
			} else {
				this.nodeLibe.replace(entry.getKey(), entry.getValue());	
			}			
			this.edgeLibe.replaceNode(entry.getKey(), entry.getValue());			
		}
	}

	/**
	 * Reads an EdgeLibrary from a line in the config file.
	 * 
	 * @param line
	 * @return
	 */
	public static EdgeLibrary readEdgeFile(Configuration config, String[] line)
	throws InvalidValueException, IOException, DuplicateException {
		String filename = line[1];

		// If auto-feature(s) and value(s) provided in line[2],
		// must have been previously declared.
		HashMap<Feature, Value> autoFeats = null;
		boolean hasAuto = line[2].contains("=");

		if (config != null && hasAuto) {
			// get auto features
			autoFeats = new HashMap<Feature, Value>();
			String[] afStrs = line[2].split(VAL_DELIM);
			for (String af : afStrs) {
				String[] autoSplit = af.split("=");
				Feature autoFeat = config.edgeLibe.getFeature(autoSplit[0]);
				if (autoFeat==null) {
					throw new InvalidValueException(
							String.format("You must declare the auto feature %s before reading the edge library.", autoSplit[0]));
				}
				Value autoValue = autoFeat.legal(autoSplit[1]);
				if (autoValue==null) {
					throw new InvalidValueException(
							String.format("Illegal value %s requested for auto feature %s.", autoSplit[1], autoSplit[0]));
				}
				// one more check to make sure we didn't request two values
				if (autoFeats.containsKey(autoFeat) && !autoFeats.get(autoFeat).equals(autoValue)) {
					throw new InvalidValueException(
							String.format("File %s: Tried to assign more than one auto value for auto feature %s.", 
									filename, autoSplit[0]));
				}
				autoFeats.put(autoFeat, autoValue);
			}
		} 
		// fail if no configuration and yet request for auto values
		else if (config==null && hasAuto) {
			throw new InvalidValueException(
					String.format("Invalid edge library request: %s", Arrays.toString(line)));
		}

		int start = -1;
		try {
			start = Integer.parseInt(line[3]);
		} catch (NumberFormatException nfe) {
			throw new InvalidValueException(
					String.format("Invalid start column ID for file '%s': '%s'", filename, line[3]));
		}

		HashSet<Integer> readCols = null;
		if (line.length > 4) {
			readCols = new HashSet<Integer>();
			String[] colS = line[4].split(VAL_DELIM);
			for (String s : colS) {
				try {
					int c = Integer.parseInt(s);
					readCols.add(c);	
				} catch (NumberFormatException nfe) {
					throw new InvalidValueException(
							String.format("Invalid column ID for feature-reading from file '%s'", filename, s));		
				}
			}
		}
		
		HashSet<Integer> unboundCols = null;
		if (line.length > 5) {
			unboundCols = new HashSet<Integer>();
			String[] colS = line[5].split(VAL_DELIM);
			for (String s : colS) {
				try {
					int c = Integer.parseInt(s);
					unboundCols.add(c);	
				} catch (NumberFormatException nfe) {
					throw new InvalidValueException(
							String.format("Invalid column ID for feature-reading from file '%s'", filename, s));		
				}
			}
		}

		// What is this??
		String srcFormat=null, tarFormat=null;
		// Source/target format string? something about allowing us to add "_RNA" here
		if (line[line.length-1].contains("%s")) {
			int i=line.length-1;
			String[] sp = line[i].split(",");
			for (String s : sp) {
				String[] again = s.split("=");
				if (again.length!=2) {
					throw new InvalidValueException(String.format(
							"Invalid format argument to edge library %s: %s",filename, s)); 
				}
				if (again[0].equalsIgnoreCase("source")) {
					srcFormat = again[1];
				} else if (again[0].equalsIgnoreCase("target")) {
					tarFormat = again[1];
				}
			}
		}
		
		File test=new File(filename);
		if (!test.exists()) {
			throw new FileNotFoundException(String.format(
					"Cannot find requested edge file %s.", filename));
		}

		EdgeLibrary el = EdgeLibrary.readEdges(filename, start, autoFeats, readCols, unboundCols, srcFormat, tarFormat);

		return el;
	}

	/**
	 * Reads a node feature as specified on a line beginning with "NFEATURE".
	 * 
	 * @param configLine	split line from config file
	 * @return	
	 */
	public static Pair<Feature, Map<String, Value>> readNodeFeature(String[] line) 
	throws InvalidValueException, IOException {

		// field 0 should be "NFEATURE"
		assert(line[0].equals(NODE_FEATURE)) : "Should be node feature?" + line[0];

		// Fields:
		//	NFEATURE	name=Type(val|...|valN)	note	filename	delimiter column
		
		if (line.length < 6) {
			throw new InvalidValueException(String.format("Not enough fields in declaration of node feature '%s'.", Arrays.toString(line)));
		}

		String declaration=line[1];
		String note=line[2];
		String filename=line[3];

		Feature f = Feature.readFeatureDeclaration(declaration, note, filename);		
		// make sure we didn't use a reserved word
		GraphFeature gf=null;
		try {
			gf = GraphFeature.valueOf(f.name());
		} catch (Exception e) {
			//ok
		}
		if (gf != null) {
			throw new InvalidValueException(
					String.format("Declared node feature name '%s' " +
							"is reserved for graphs.", f.name()));
		}		

		String delim = line[4];

		Value defVal = null;

		int col=-1;
		String colStr = line[5];
		// type of feature declaration - default value, or value in file?
		if (colStr.contains("=")) {
			String[] sp = colStr.split("=");
			colStr = sp[0];
			defVal = f.legal(sp[1]);
		}

		try {
			col = Integer.parseInt(colStr);
		} catch (NumberFormatException e) {
			throw new InvalidValueException(
					String.format("Bad column ID for feature '%s': %s", f.name(), line[5]));					
		}

		Map<String, Value> values = null;
		if (defVal == null) {
			// may throw InvalidValueException or IOException
			values = Feature.readNodeFeature(f, filename, delim, col);
		} else {
			values = Feature.readNodeFeatureDefaultValue(f, defVal, filename, delim, col);
		}

		return new Pair<Feature, Map<String, Value>>(f, values);
	}

	public Graph runNodeFilters(FilterItemMode mode) throws DuplicateException {
		Graph g = Graph.createFromEdgeLibrary(this.edgeLibrary());
		switch(mode) {
		case AND:
			g=GraphUtils.filterAnd(g, this.nodeFilters.values(), this.nodeLibe); break;
		case OR:
			g=GraphUtils.filterOr(g, this.nodeFilters.values(), this.nodeLibe); break;
		}
		return g;

	}

}
