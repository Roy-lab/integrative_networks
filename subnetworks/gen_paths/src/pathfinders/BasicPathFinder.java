package pathfinders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import structures.Configuration;
import structures.Graph;
import structures.PairDirectory;
import structures.Path;
import structures.PathManager;
import utilities.DebugTools;
import exceptions.InvalidValueException;
import filters.EdgeFilterManager;
import filters.NodeFilterManager;

/**
 * A basic PathFinder will find all paths up to a given depth that:
 * -start with a 'start' accepted node
 * -end with an 'end' accepted node AND contain only ONE node from the 'end' set.
 * By default, search continues after the first accepted end node, so that multiple endpoints can appear in the path. 
 * However, we can tell the PathFinder to operate in "save and stop"
 *
 * 
 * If PairDirectory is provided, we'll also filter based on the directionality (only? sign would need to be implemented).
 * 
 * @author chasman
 *
 */
public class BasicPathFinder extends PathFinder {	

	protected NodeFilterManager start, end;
	protected final int depth;
	
	// optional PairDirectory for direction filtering
	protected PairDirectory pairs;
	
	// what to do when endpoint found?
	// default behavior is to save and continue
	protected boolean stopAtFirstEndpoint=false;
	
	/*
	 * Keep a set of edge filter managers to apply to paths.
	 */
	protected Collection<EdgeFilterManager> pathEfms;
	

	/**
	 * 
	 * @param name	name of pathfinder, specified in config
	 * @param start	the manager that accepts start-type nodes 
	 * @param end	manager that accepts end-type nodes
	 * @param depth	total search depth, in interactions
	 */
	public BasicPathFinder(String name, NodeFilterManager start, NodeFilterManager end, int depth) {
		this.name=name;
		this.start=start;
		this.end=end;
		this.depth=depth;
	}

	/**
	 * Alternate constructor lets us specify search behavior. 
	 * By default, search stops when the first potential endpoint (specified by "end" manager) is found - each path will contain only one.
	 * If "false" provided, each path can contain multiple nodes from the "end" class. Search stops only when depth limit is hit.
	 * @param name
	 * @param start
	 * @param end
	 * @param depth
	 * @param stopAtFirstEndpoint	if true, halt search when first endpoint found. otherwise, save path and continue.
	 */
	public BasicPathFinder(String name, NodeFilterManager start, NodeFilterManager end, int depth, boolean stopAtFirstEndpoint) {
		this(name, start, end, depth);
		this.stopAtFirstEndpoint=stopAtFirstEndpoint;		
	}

	
	/**
	 * This constructor lets us specify a PairDirectory against which we can test candidate paths.
	 * For example, the PairDirectory may specify a required order of a pair of nodes. Paths that violate
	 * the order will be discarded. 
	 * @param name
	 * @param start
	 * @param end
	 * @param depth
	 * @param stopAtFirstEndpoint
	 * @param pairs
	 */
	public BasicPathFinder(String name, NodeFilterManager start, NodeFilterManager end, int depth, boolean stopAtFirstEndpoint, PairDirectory pairs) {
		this(name, start, end, depth, stopAtFirstEndpoint);
		this.pairs=pairs;
	}
	
	
	/*
	 * The basic PathFinder paths start with items in the "start" set
	 * and end with an item in the "end" set. 
	 * @param g	background network graph
	 * @return	pathmanager containing paths found in the supplied graph
	 */
	public PathManager findPaths(Graph g) {
		PathManager found = new PathManager();		

		// stop if depth==0
		if (this.depth==0) return found;

		// get the start nodes
		List<String> startNodes = start.apply(g.nodes());

		// search for each start node
		for (String node : startNodes) {	
			if (!g.contains(node)) {
				if (DebugTools.DEBUG) System.out.println("Node not in graph: " + node);
				continue;		
			}

			PathManager npaths = this.findPaths(g, node, this.depth);

			if (DebugTools.DEBUG && npaths.size() > 0) {
				System.out.println(String.format("Found %d paths for starting node %s.", 
						npaths.size(), node));
			}

			// npaths is null if node not in graph
			if (npaths != null) found.addAll(npaths);			
		}

		return found;
	}
	
	/**
	 * Not currently implemented for this pathfinder
	 * @param g
	 * @param addDepth
	 * @param stop
	 * @return
	 */
	public PathManager findPathsIterative(Graph g) {
		assert(false):
			"Iterative-deepening search option not implemented for BasicPathFinder.";
		return null;
	}
	
	/**
	 * Not currently implemented for this pathfinder
	 * @param g
	 * @param addDepth
	 * @param stop
	 * @return
	 */
	public PathManager findPathsIterative(Graph g, int maxDepth, double stop) {
		assert(false):
			"Iterative-deepening search option not implemented for BasicPathFinder.";
		return null;
	}

	
	/**
	 * Given a path, this method determines how to proceed.
	 *  
	 * @param p	path to verify
	 * @param depth	remaining edge length budget
	 * @return	PathStatus.STOP if path violates supplied pair ordering or no remaining edge budget; 
	 * PathStatus.SAVE_AND_STOP if last node is an endpoint and (stopAtFirstEndpoint==true or no remaining budget) 
	 * PathStatus.SAVE_AND_CONTINUE if last node is an endpoint and stopAtFirstEndpoint==false
	 * PathStatus.CONTINUE otherwise
	 */
	protected PathStatus verify(Path p, int depth) {
		// if a pair directory is specified: is final node compatible with all others wrt the PairDirectory?
		// stop immediately if violated
		boolean dirOkay = this.pairs==null ? true : testPairOrder(p);
		if (!dirOkay) return PathStatus.STOP;
		
		// is final node in the end set? 
		String last = p.getNode(-1);
		boolean isEndpoint = this.end.accept(last);

		// if yes AND there is at least one edge, save the path.
		// do we stop or continue?
		if (isEndpoint && p.edgeLength() > 0) {
			if (this.stopAtFirstEndpoint) {
				return PathStatus.SAVE_AND_STOP;
			}
			else return PathStatus.SAVE_AND_CONTINUE;		
		}
		
		// ran out of depth - stop.
		if (depth==0) return PathStatus.STOP;

		// keep going otherwise.
		return PathStatus.CONTINUE;		
	}
	
	/**
	 * Checks to make sure order of PairDirectory not violated by the LAST
	 * element of this path. (Assume that we've already tested the others
	 * as we added one node at a time.)
	 * 
	 * @param p
	 * @return false if any node is required to appear below the last node.
	 */
	protected boolean testPairOrder(Path p) {
		String last = p.getNode(-1);
		for (int i = 0; i < p.edgeLength(); i++ ) {
			String q = p.getNode(i);
			PairDirectory.PartialOrder required = this.pairs.getOrder(q, last);
			if (required==PairDirectory.PartialOrder.BELOW) return false;
		}
		return true;
	}
	 
	public String toString() {
		String indir = this.pairs==null? "" : ", PairDirectory " + pairs.filename();
		return String.format("BasicPathFinder %s start=%s, end=%s, depth=%d%s",
				this.name(), this.start.name(), this.end.name(), this.depth, indir);
	}

	/**
	 * Reads a pathfinder given a line and config.
	 * 'start' and 'end' are NodeFilterManagers.
	 * 5 is the depth.
	 * Optionally:
	 * - can specify a pairdirectory and test if paths violate order specified in the directory
	 * - can instruct the pathfinder to save and stop when first endpoint found (rather than continuing and allowing multiple endpoints per path).
	 * 
	 * PATHFINDER	BasicPathFinder	start	end	5	[name of previously declared pair directory] [SAVE_AND_STOP]
	 * 
	 * @param line
	 * @param config
	 * @return
	 */
	public static BasicPathFinder readPathFinder(String[] line, Configuration config)
	throws InvalidValueException {
		String err = "";

		if (line.length < 6 || !line[2].equals("BasicPathFinder")) {
			throw new InvalidValueException("Does not declare a BasicPathFinder: " + Arrays.toString(line));
		}

		String name = line[1];
		
		NodeFilterManager start, end;
		int depth=0;

		start = config.getNodeFilterManager(line[3]);
		if (start==null) {
			err="Invalid NodeFilterManager: " + line[3];
		}

		end = config.getNodeFilterManager(line[4]);
		if (end==null) {
			err="Invalid NodeFilterManager: " + line[4];
		}

		try {
			depth = Integer.parseInt(line[5]);	
		} catch (NumberFormatException nfe) {
			err="Invalid depth: " + line[5];
		}
		
		PairDirectory pairs = null;	// default: don't test any pair ordering
		boolean stopAtFirstEndpoint=false;	// default: save and continue.
		if (line.length > 6) {
			// what kind of additional settings do we have?
			for (int i=6; i < line.length; i++) {
				// check if pair directory.
				pairs = config.getPairDirectory(line[i]);
				
				// if not, check if "stop" anywhere in the item.
				if (pairs==null && line[i].toUpperCase().contains("STOP")) {
					stopAtFirstEndpoint=true;
				} 
				// if not, then we assume the pair directory was incorrectly requested.
				else if (pairs==null) {
					err="Invalid pair directory: " + line[i];
				}
			}
		}
		
		if (err.length() > 0 ) {
			throw new InvalidValueException(err);
		}

		if (pairs==null)
			return new BasicPathFinder(name, start, end, depth, stopAtFirstEndpoint);
		else 
			return new BasicPathFinder(name, start, end, depth, stopAtFirstEndpoint, pairs);
	}

	@Override
	public void addEdgeFilterManager(EdgeFilterManager efm) {
		if (this.pathEfms==null) {
			this.pathEfms=new ArrayList<EdgeFilterManager>();
		}
		this.pathEfms.add(efm);
		
	}

	@Override
	protected Collection<EdgeFilterManager> getEdgeFilterManagers() {
		return this.pathEfms;
	}

}
