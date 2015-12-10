package pathfinders;

import java.util.Arrays;

import structures.Configuration;
import structures.PairDirectory;
import structures.Path;
import exceptions.InvalidValueException;
import filters.EdgeFilterManager;
import filters.NodeFilterManager;

/**
 * Like the regular basicpathfinder, but this one lets us take some edges without counting them against the depth.
 * For example: we may wish to get protein->complex edges for "free", suggesting that the participation of the
 * protein in the path is due to its role in the complex.  
 * @author chasman
 *
 */
public class BasicFreeEdgePathFinder extends BasicPathFinder {
	/*
	 * Any edge accepted by this filtermanager will not count against path depth
	 */
	protected EdgeFilterManager filterMan; 
	
	public BasicFreeEdgePathFinder(String name, NodeFilterManager start, 
			NodeFilterManager end, int depth, EdgeFilterManager filterMan) {
		super(name, start, end, depth);
		this.filterMan=filterMan;
	}	

	/**
	 * 
	 * @param name
	 * @param start
	 * @param end
	 * @param depth
	 * @param stopAtFirstEndpoint	when endpoint found: true means "save and continue", false means "save and stop"
	 * @param filterMan
	 */
	public BasicFreeEdgePathFinder(String name, NodeFilterManager start, 
			NodeFilterManager end, int depth, boolean stopAtFirstEndpoint, EdgeFilterManager filterMan) {
		super(name, start, end, depth, stopAtFirstEndpoint);
		this.filterMan=filterMan;
	}	
	
	/**
	 * 
	 * @param name
	 * @param start
	 * @param end
	 * @param depth
	 * @param stopAtFirstEndpoint
	 * @param pairs
	 * @param filterMan
	 */
	public BasicFreeEdgePathFinder(String name, NodeFilterManager start, 
			NodeFilterManager end, int depth, boolean stopAtFirstEndpoint, PairDirectory pairs, EdgeFilterManager filterMan) {
		super(name, start, end, depth, stopAtFirstEndpoint, pairs);
		this.filterMan=filterMan;
	}
	
	/**
	 * Outcomes:
	 * 	SAVE_AND_STOP:	Path terminates in endpoint
	 * 	SAVE_AND_CONTINUE: Save current path and continue - but won't continue for free!
	 *  CONTINUE: Depth not yet achieved
	 *  CONTINUE_FOR_FREE: Most recent edge accepted by filter
	 * 	STOP: Depth achieved but not accepted by final filter
	 */
	@Override
	protected PathStatus verify(Path p, int depth) {
		PathStatus plain = super.verify(p, depth);
		
		//assert(plain != PathStatus.SAVE_AND_CONTINUE) :
		//	"Case for SAVE_AND_CONTINUE in regular BasicFreeEdgePathfinder not implemented.";
		
		// if passed the vanilla pathfinder with "continue" status,
		// or stopped because depth == 0,
		// check on the edge filter manager.
		if (plain==PathStatus.CONTINUE || (depth==0 && plain==PathStatus.STOP)) {
			if (filterAccept(p)) {
				return PathStatus.CONTINUE_FOR_FREE;
			} else {
				// otherwise, echo vanilla pathfinder.
				return plain;
			}
		} else {
			// echo vanilla pathfinder
			return plain;
		}
	}
	
	/**
	 * Accept if the edgefiltermanager accepts the last edge.
	 * Don't accept if empty.
	 * @param p
	 * @return
	 */
	protected boolean filterAccept(Path p) {
		if (p.edgeLength() == 0) return false;
		else return this.filterMan.accept(p.getEdge(-1));	
	}

	public String toString() {
		String indir = this.pairs==null? "" : ", Indirectory " + pairs.filename();		
		return String.format("BasicFreeEdgePathFinder %s start=%s, end=%s, depth=%d, free_edge_filter=%s%s", 
				this.name, this.start.name(), this.end.name(), this.depth, this.filterMan.name(), indir);
	}
	
	/**
	 * Reads a pathfinder given a line and config.
	 * 'start', 'end', are NodeFilterManagers.
	 * 'free' is an EdgeFilterManager
	 * 5 is the depth.
	 * Optionally:
	 * - can specify a pairdirectory and test if paths violate order specified in the directory
	 * - can instruct the pathfinder to save and stop when first endpoint found (rather than continuing and allowing multiple endpoints per path).
	 * 
	 * PATHFINDER	BasicFreeEdgePathFinder	start	end	5	free	[name of previously declared pair directory] [SAVE_AND_STOP]
	 * 
	 * @param line
	 * @param config
	 * @return
	 */
	public static BasicFreeEdgePathFinder readPathFinder(String[] line, Configuration config)
	throws InvalidValueException {
		String err = "";

		if (line.length < 7 || !line[2].equals("BasicFreeEdgePathFinder")) {
			throw new InvalidValueException("Does not declare a BasicFreeEdgePathFinder: " + Arrays.toString(line));
		}
		
		String name = line[1];

		NodeFilterManager start, end;
		EdgeFilterManager free;
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
			err="Invalid depth:" + line[5];
		}
		
		free = config.getEdgeFilterManager(line[6]);
		if (end==null) {
			err="Invalid EdgeFilterManager: " + line[6];
		}
		
		PairDirectory pairs = null;	// default: don't test any pair ordering
		boolean stopAtFirstEndpoint=false;	// default: save and continue.
		if (line.length > 7) {
			// what kind of additional settings do we have?
			for (int i=7; i < line.length; i++) {
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
			return new BasicFreeEdgePathFinder(name, start, end, depth, stopAtFirstEndpoint, free);
		
		else return new BasicFreeEdgePathFinder(name, start, end, depth, stopAtFirstEndpoint, pairs, free);
	}
}
