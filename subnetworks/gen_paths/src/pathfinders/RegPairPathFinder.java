package pathfinders;

import java.util.Arrays;

import structures.Configuration;
import structures.Edge;
import structures.Graph;
import structures.PairDirectory;
import structures.Path;
import structures.PathManager;
import exceptions.InvalidValueException;
import filters.EdgeFilterManager;

/**
 * Finds paths from source to target (defined by a PairDirectory)
 * in which the final interaction is accepted by an EdgeFilterManager.
 * (e.g.: the final interaction must represent transcriptional
 * regulation.)
 * 
 * This kind of pathfinder will produce both BranchyPaths and regular Paths.
 * (BranchyPath - collapse paths together if same edges up until the end.)
 * @author chasman
 *
 */
public class RegPairPathFinder extends PairPathFinder {
	// Filters the final edge in the path
	protected EdgeFilterManager finalEdgeFilter;
	
	public RegPairPathFinder(String name, PairDirectory index, 
			EdgeFilterManager finalEdgeManager, int depth) {
		super(name, index, depth);
		this.finalEdgeFilter=finalEdgeManager;
	}
	
	
	/**
	 * An accepted ends when we reach one of the source's targets AND
	 * the final edge is accepted by the EdgeFilterManager.
	 * If the edge isn't accepted by the EdgeFilterManager, we'll just 
	 * continue searching. We might hit another target. 
	 * 
	 * Stop if we run out of depth.
	 */
	@Override
	protected PathStatus verify(Path p, int depth) {
		// Are first and final nodes a pair?
		PathStatus superStatus = super.verify(p, depth);
		
		assert(superStatus != PathStatus.SAVE_AND_CONTINUE) :"PathStatus.Save_and_continue not implemented";
		
		// If connects source-target pair, then...
		if (superStatus==PathStatus.SAVE_AND_STOP) {
			// Is this an accepted edge? If so, we're done! Yay!
			Edge finalE = p.getEdge(-1);
			if (this.finalEdgeFilter.accept(finalE)) {
				return PathStatus.SAVE_AND_STOP;
			} else {
				// Otherwise, we'll keep looking for another target.
				return PathStatus.CONTINUE;
			}
		} else {
			// Otherwise, echo the super class's decision.
			return superStatus;
		}	
	}
	
	/**
	 * Runs iterative deepening if we have the right flag set
	 */
	@Override
	public PathManager findPaths(Graph g) {
		if (this.iterativeDeepening) {
			return findPathsIterative(g);
		} else
			return findPaths(g);
	}
	
	/**
	 * Reads a pairpathfinder given a line and config.
	 * It takes a PairIndex object to designate start and endpoints.
	 * 5 is the depth.
	 * If depth given as range, we will use iterative deepening instead (not implemented yet)
	 * 
	 * PATHFINDER	name	PairPathFinder	pairIndex	5
	 * (not implemented yet)
	 * PATHFINDER	name	RegPairPathFinder	pairIndex	5 7
	 * 
	 * @param line
	 * @param config
	 * @return
	 */
	public static PairPathFinder readPathFinder(String[] line, Configuration config)
	throws InvalidValueException {
		String err = "";

		if (line.length < 5 || !line[2].equals("RegPairPathFinder")) {
			throw new InvalidValueException("Does not declare a RegPairPathFinder: " + Arrays.toString(line));
		}

		String name = line[1];
		
		PairDirectory pairs;
		int depth=0;

		pairs = config.getPairDirectory(line[3]);
		if (pairs==null) {
			err="Invalid PairDirectory: " + line[3];
		}
		
		EdgeFilterManager finalEdgeMan = config.getEdgeFilterManager(line[4]);
		if (finalEdgeMan==null) {
			err=String.format("%sInvalid PairDirectory: %s", err, line[4]);
		}

		try {
			depth = Integer.parseInt(line[5]);	
		} catch (NumberFormatException nfe) {
			err="Invalid depth:" + line[5];
		}		
		
		if (err.length() > 0 ) {
			throw new InvalidValueException(err);
		}

		return new RegPairPathFinder(name, pairs, finalEdgeMan, depth);
	}
	
	
	public String toString() {	
		return String.format("RegPairPathFinder %s pairs=%s, final_edge_manager=%s, depth=%d", 
				this.name, this.stPairs.filename(), this.finalEdgeFilter.name(), this.depth);
	}

}
