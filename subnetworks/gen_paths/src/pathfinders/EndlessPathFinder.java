package pathfinders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import structures.Configuration;
import structures.Edge;
import structures.Graph;
import structures.Graph.RType;
import structures.PairDirectory;
import structures.Path;
import structures.PathManager;
import utilities.DebugTools;
import exceptions.InvalidValueException;
import filters.EdgeFilterManager;
import filters.NodeFilterManager;

/**
 * A Endless EndlessPathFinder will find all paths up to a given depth that:
 * -start with a 'start' accepted node
 * 
 * If an Indirectory is provided, we'll also filter based on the directionality (only?).
 * 
 * @author chasman
 *
 */
public class EndlessPathFinder extends PathFinder {	

	protected NodeFilterManager start;
	protected final int depth;
	
	// optional Indirectory for direction filtering
	protected PairDirectory indie;
	
	
	/*
	 * Keep a set of edge filter managers to apply to paths.
	 */
	protected Collection<EdgeFilterManager> pathEfms;

	public EndlessPathFinder(String name, NodeFilterManager start, int depth) {
		this.name=name;
		this.start=start;
		this.depth=depth;
	}

	public EndlessPathFinder(String name, NodeFilterManager start, int depth, PairDirectory indie) {
		this(name, start, depth);
		this.indie=indie;
	}
	
	

	/*
	 * The Endless PathFinder paths start with items in the "start" set
	 * and stops searching only when depth runs out.
	 * @param g
	 * @param depth
	 * @return
	 */
	public PathManager findPaths(Graph g) {
		PathManager found = new PathManager();		

		// stop if depth==0
		if (depth==0) return found;

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
	 * Finds paths starting at a given node.
	 * Returns null if the requested node isn't in the graph.
	 * @param start
	 * @param depth
	 * @return
	 */
	public PathManager findPaths(Graph g, String start, int depth) {
		// stop immediately and return if start not in graph
		if (!g.contains(start)) return null;

		PathManager pm = new PathManager();

		Path init = new Path(start);
		PathStatus verify = this.verify(init, depth);
		if (verify==PathStatus.SAVE_AND_STOP) {
			pm.add(init, this.name());
		} else if (verify==PathStatus.CONTINUE ){
			pm = this.search(g, new Path(start), depth);
		} else {
			assert(false):
				"Shouldn't receive any other path status.";
		}

		return pm;		
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
			"Iterative-deepening search option not implemented for EndlessPathFinder.";
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
			"Iterative-deepening search option not implemented for EndlessPathFinder.";
		return null;
	}

	/**
	 * Searches out from the last node in the given path.
	 * @param g
	 * @param curNode
	 * @param currPath
	 * @param depth
	 * @return
	 */
	public PathManager search(Graph g, Path currPath, int depth) {
		PathManager found = new PathManager();

		// if depth == 0, can return current path.
		if (depth==0) {
			found.add(currPath, this.name());
			return found;
		}

		String last = currPath.getNode(-1);

		// check outgoing and undirected edges
		HashSet<Edge> further = new HashSet<Edge>(g.incident(last, RType.OUTGOING));
		further.addAll(g.incident(last, RType.UNDIRECTED));
		
		// if no outgoing edges, then save and stop here.
		if (further.size()==0) {
			found.add(currPath, this.name());
		}

		for (Edge e : further) {
			Path next = currPath.copyAdd(e);

			// if addition fails, the edge must have introduced a cycle.
			// keep going.
			if (next==null) continue;

			// otherwise, check the path against the PathFinder!
			PathStatus verify = this.verify(next, depth);
			assert(verify != PathStatus.SAVE_AND_CONTINUE):
				"Not implemented.";

			if (verify==PathStatus.SAVE_AND_STOP) {
				// save the path and stop! woo
				found.add(next, this.name()); 
			} else if (verify==PathStatus.CONTINUE) {
				// keep looking
				PathManager deeper = search(g, next, depth-1);
				found.addAll(deeper);
			} else if (verify==PathStatus.STOP) {
				continue;
			}			
		}
		
		return found;
	}

	/**
	 * The EndlessPathFinder stops searching immediately when an item in 
	 * the "end" set is located.
	 * @param p
	 * @param depth
	 * @return
	 */
	protected PathStatus verify(Path p, int depth) {
		// if an indirectory is specified: is final node compatible with all others wrt the indirectory?
		// stop immediately if violated
		boolean dirOkay = this.indie==null ? true : testIndirectoryOrder(p);
		if (!dirOkay) return PathStatus.STOP;

		// ran out of depth - accept!
		if (depth==0) 
			return PathStatus.SAVE_AND_STOP;

		// keep going otherwise.
		return PathStatus.CONTINUE;		
	}
	
	/**
	 * Checks to make sure order of Indirectory not violated by the LAST
	 * element of this path. (Assume that we've already tested the others
	 * as we added one node at a time.)
	 * 
	 * @param p
	 * @return false if any node is required to appear below the last node.
	 */
	protected boolean testIndirectoryOrder(Path p) {
		String last = p.getNode(-1);
		for (int i = 0; i < p.edgeLength(); i++ ) {
			String q = p.getNode(i);
			PairDirectory.PartialOrder required = this.indie.getOrder(q, last);
			if (required==PairDirectory.PartialOrder.BELOW) return false;
		}
		return true;
	}
	 
	public String toString() {
		String indir = this.indie==null? "" : ", Indirectory " + indie.filename();
		return String.format("EndlessPathFinder %s start=%s, depth=%d%s",
				this.name(), this.start.name(),  this.depth, indir);
	}

	/**
	 * Reads a pathfinder given a line and config.
	 * 'start' and 'end' are NodeFilterManagers.
	 * 5 is the depth.
	 * 
	 * PATHFINDER	EndlessPathFinder	start	end	5
	 * 
	 * @param line
	 * @param config
	 * @return
	 */
	public static EndlessPathFinder readPathFinder(String[] line, Configuration config)
	throws InvalidValueException {
		String err = "";

		if (line.length < 5 || !line[2].equals("EndlessPathFinder")) {
			throw new InvalidValueException("Does not declare a EndlessPathFinder: " + Arrays.toString(line));
		}

		String name = line[1];
		
		NodeFilterManager start;
		int depth=0;

		start = config.getNodeFilterManager(line[3]);
		if (start==null) {
			err="Invalid NodeFilterManager: " + line[3];
		}

		try {
			depth = Integer.parseInt(line[4]);	
		} catch (NumberFormatException nfe) {
			err="Invalid depth:" + line[4];
		}
		
		PairDirectory indie = null;
		if (line.length > 6) {
			indie = config.getPairDirectory(line[5]);
		}
		
		if (err.length() > 0 ) {
			throw new InvalidValueException(err);
		}

		if (indie==null)
			return new EndlessPathFinder(name, start, depth);
		else 
			return new EndlessPathFinder(name, start, depth, indie);
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
