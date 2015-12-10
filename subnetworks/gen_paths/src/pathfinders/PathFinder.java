package pathfinders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import structures.BranchyPath;
import structures.Configuration;
import structures.Edge;
import structures.Graph;
import structures.Graph.RType;
import structures.Path;
import structures.PathManager;
import exceptions.InvalidValueException;
import filters.EdgeFilterManager;

/**
 * The abstract PathFinder class knows how to do a depth-limited,
 * depth-first search. Subclasses define stopping conditions in the
 * "verify(path, depth)" method by returning PathStatus enums.
 * @author chasman
 *
 */
public abstract class PathFinder {
	
	/**
	 * At each search step, we check the current
	 * path and return one of these status codes.
	 * 
	 * Note that "save and continue for free" is not currently an option.
	 * @author chasman
	 *
	 */
	public static enum PathStatus {
		SAVE_AND_CONTINUE,	// store the current path and keep searching 
		SAVE_AND_STOP,	// store the current path but don't keep searching
		STOP,		// stop search without saving path
		CONTINUE,	// keep extending the path
		CONTINUE_FOR_FREE; // keep extending the path, without counting against depth 
	}
	
	/**
	 * We can allow PathFinders to collapse paths into BranchyPaths
	 * according to some rule.
	 * @author chasman
	 *
	 */
	public static enum CollapseMode {
		NO, // Don't allow collapsing - only produce regular linear paths
		ALL_BUT_LAST; // Collapse two (or more) paths if all edges but the final one
						// are the same.
	}
	
	/**
	 * Contains all of the currently implemented PathFinder subclasses.
	 * @author chasman
	 *
	 */
	public enum Implemented {
		/* starts with a node in one category, stops with a node in another category */ 
		BASICPATHFINDER,
		/* starts with a node in one category, stops with a node in another category,
		 * and additionally requires that the path's nodes are accepted by another node filter */
		BASICFILTEREDPATHFINDER,
		/* a basic path finder that incorporates specified edge categories for free */  
		BASICFREEEDGEPATHFINDER,
		/* starts with a node from one category and continues for a given depth */
		ENDLESSPATHFINDER,
		/* finds paths between specific source-target pairs */
		PAIRPATHFINDER,
		/* ...  in which the last edge is between a candidate regulator
		 * and the target
		 */
		SOURCEREGTARGETPATHFINDER,
		/* finds paths between specific source-target pairs, AND additionally
		 * require that the last edge passes a given edge filter. */
		REGPAIRPATHFINDER
	}
	
	/*
	 * The name of the pathfinder, as given by the user
	 * via the configuration file.
	 */
	protected String name;
	
	/*
	 * Do we allow collapsing into BranchyPathways?
	 * By default, no...
	 */
	protected CollapseMode collapseMode=CollapseMode.NO; //CollapseMode.ALL_BUT_LAST;
	
	/**
	 * Return the name of the PathFinder. (defined in the config file.)
	 * @return	pathfinder's name
	 */
	public String name() {
		return this.name;
	}
	
	/**
	 * Adds an edge filter manager. May be applied to all paths before return from "findPaths".
	 * @param efm
	 */
	public abstract void addEdgeFilterManager(EdgeFilterManager efm);
	
	/**
	 * Find paths to a given depth.
	 * The basic PathFinder paths start with items in the "start" set
	 * and stops searching immediately when an item in the "end" set is 
	 * located.
	 * @param g
	 * @param depth
	 * @return	a PathManager that contains the paths
	 */
	public abstract PathManager findPaths(Graph g);
	
	/**
	 * Gets this object's list of edge filter managers.
	 * @return
	 */
	protected abstract Collection<EdgeFilterManager> getEdgeFilterManagers();
	
	/**
	 * Performs a depth-limited, depth-first search starting with 
	 * a given node. Stops according to the subclass's verify(path, depth)
	 * return value.
	 * 
	 * Returns null if the requested node isn't in the graph.
	 * @param start	starting node
	 * @param depth	max depth in edges
	 * @return	the path management object containing all of the found paths
	 */
	public PathManager findPaths(Graph g, String start, int depth) {
		// stop immediately and return if start not in graph
		if (!g.contains(start)) return null;

		PathManager pm = new PathManager();

		Path init = new Path(start);
		PathStatus verify = this.verify(init, depth);

		boolean validStatus=false;
		// check to see if the starting node is valid for the pathfinder subclass.
		if (verify==PathStatus.SAVE_AND_STOP || verify==PathStatus.SAVE_AND_CONTINUE) {
			validStatus=true;
			pm.add(init, this.name());
		} 

		if (verify==PathStatus.CONTINUE || verify==PathStatus.SAVE_AND_CONTINUE || verify==PathStatus.CONTINUE_FOR_FREE){
			validStatus=true;
			// we have the OK to start looking!
			pm = this.search(g, init, depth);
		}
		
		assert(validStatus):"Shouldn't have received status for starting node: " + verify.toString();
		
		return pm;		
	}
	
	/** 
	 * Finds paths in an iterative deepening search.
	 * For each starting point, searches for paths up to an internally known maxDepth.
	 * "stop" is a stopping condition known by the instance of the pathfinder. 
	 * 
	 * @param g
	 * @return
	 */
	public abstract PathManager findPathsIterative(Graph g);
	public abstract PathManager findPathsIterative(Graph g, int maxDepth, double stop);
	
	/**
	 * Checks against the pathfinder's criteria for stopping conditions.
	 * @param p	current path
	 * @param depth	max depth remaining
	 * @return the path status, which explains how to proceed
	 */
	protected abstract PathStatus verify(Path p, int depth);
	
	/**
	 * Recursively continues a depth-limited, depth-first search from the 
	 * last node in the current path.
	 * 
	 * @param g	our graph
	 * @param currPath	the path so far
	 * @param depth	maximum depth remaining (if 0, stop immediately)
	 * @return	paths found from this point forward
	 */
	public PathManager search(Graph g, Path currPath, int depth) {
		PathManager found = new PathManager();

		// if depth == 0, can stop. return no paths.
		if (depth==0) return found;

		// get the last node
		String last = currPath.getNode(-1);

		// check outgoing and undirected edges
		HashSet<Edge> further = new HashSet<Edge>(g.incident(last, RType.OUTGOING));
		further.addAll(g.incident(last, RType.UNDIRECTED));

		// for each outgoing path...
		
		// keep track of any paths that terminate on the next edge
		// we may wish to collapse them into a BranchyPath
		ArrayList<Edge> potentialBranches = new ArrayList<Edge>();
		
		
		for (Edge e : further) {
			// make a copy of the current path PLUS the new edge.
			Path next = currPath.copyAdd(e);
			
			if (e.nodes().contains("YOR141C") && e.nodes().contains("YLR113C")) {
				System.out.println("pause");
			}

			// If addition fails, the edge must have introduced a cycle.
			// skip this edge.
			if (next==null) continue;

			// otherwise, check the path against the PathFinder subclass.
			PathStatus verify = this.verify(next, depth);
			
			// save path.
			if (verify==PathStatus.SAVE_AND_STOP || verify==PathStatus.SAVE_AND_CONTINUE) {
				// Save the path and stop! woo
				// If we are trying to collapse paths, then we'll want to
				// just grab the edge.
				if (this.collapseMode==CollapseMode.ALL_BUT_LAST) {
					potentialBranches.add(e);	
				} else {
					// But, usually we just save the path.
					found.add(next, this.name());
				}				
			} 
			
			// Stop search
			if (verify ==PathStatus.SAVE_AND_STOP || verify==PathStatus.STOP) {
				continue;
			}
			
			//continue?
			if (verify == PathStatus.SAVE_AND_CONTINUE || verify==PathStatus.CONTINUE) {
				// Keep looking
				PathManager deeper = search(g, next, depth-1);
				found.addAll(deeper);
			} 
			// Continue without counting against depth
			// Don't currently have "save and continue for free"...
			else if (verify == PathStatus.CONTINUE_FOR_FREE) {
				PathManager deeper = search(g, next, depth);
				found.addAll(deeper); 
			} 
						
		}
		
		// Should we try to collapse the outgoing paths into BranchyPaths?
		// This mode is chosen by the active implementation/extension of PathFinder.
		if (this.collapseMode==CollapseMode.ALL_BUT_LAST && potentialBranches.size() > 0 ) {
			Path next;
			// If only one, don't collapse it.
			if (potentialBranches.size() == 1) {
				next = currPath.copyAdd(potentialBranches.get(0));
			} else {
				HashSet<String> termini = new HashSet<String>();
				HashSet<Edge> termEdges = new HashSet<Edge>(potentialBranches);
				for (Edge e : potentialBranches) {
					// get the terminal node - the one that isn't the current node
					for (String n : e.nodes()) {
						if (!n.equals(last)) {
							termini.add(n);
						}
					}
				}
				next = new BranchyPath(currPath, termini, termEdges);
			}
			// add!
			found.add(next, this.name());
		}		
		
		// run post-processing, if implemented
		found=this.applyPostProcessing(found);
		
		// run edge filter managers (specified in config)
		if (this.getEdgeFilterManagers() != null) {
			for (EdgeFilterManager efm : this.getEdgeFilterManagers()) {
				PathManager newFound = PathManager.applyEdgeFilter(found, efm);
				found=newFound;
			}
		}		
			
		return found;
	}
	
	/**
	 * Runs any post-processing filters that the PathFinder has implemented.
	 * By default, don't do anything.
	 * @param found
	 * @return
	 */
	protected PathManager applyPostProcessing(PathManager found) {
		return found;
	}
	
	
	/**
	 * Reads a PathFinder from a config line.
	 * The specific PathFinder subclass is described in the third column. 
	 * Example:
	 * PATHFINDER	name	BasicPathFinder	...
	 * @param line
	 * @param config
	 * @return
	 */
	public static PathFinder readPathFinder(String[] line, Configuration config)
	throws InvalidValueException {
		if (line.length < 2) {
			throw new InvalidValueException("Invalid pathfinder declaration: " + Arrays.toString(line));
		}
		
		Implemented imp = null;
		try {
			imp = Implemented.valueOf(line[2].toUpperCase());
		} catch (IllegalArgumentException iae) {
			throw new InvalidValueException("Unimplemented pathfinder type: " + Arrays.toString(line));
		}
		
		PathFinder pf=null;
		switch(imp) {
		case BASICPATHFINDER: 
			pf = BasicPathFinder.readPathFinder(line, config); break;
		case BASICFILTEREDPATHFINDER: 
			pf = BasicFilteredPathFinder.readPathFinder(line, config); break;
		case BASICFREEEDGEPATHFINDER:
			pf = BasicFreeEdgePathFinder.readPathFinder(line, config); break;
		case ENDLESSPATHFINDER:
			pf = EndlessPathFinder.readPathFinder(line, config); break;
		case PAIRPATHFINDER:
			pf = PairPathFinder.readPathFinder(line, config); break;
		case REGPAIRPATHFINDER:
			pf = RegPairPathFinder.readPathFinder(line, config); break;
		case SOURCEREGTARGETPATHFINDER:
			pf = SourceRegTargetPathFinder.readPathFinder(line, config); break;
		}					
		return pf;
	}
}
