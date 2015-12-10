package pathfinders;

import java.util.Arrays;

import structures.Configuration;
import structures.PairDirectory;
import structures.Path;
import exceptions.InvalidValueException;
import filters.NodeFilterManager;

/**
 * Differs from the BasicPathFinder only in that it applies 
 * a third NodeFilterManager to the contents of a basic-accepted path before
 * acceptance.
 * 
 * I don't think I've used this one at all...
 * @author chasman
 *
 */
public class BasicFilteredPathFinder extends BasicPathFinder {
	
	protected NodeFilterManager tester; 
	
	public BasicFilteredPathFinder(String name, NodeFilterManager start, 
			NodeFilterManager end, int depth, NodeFilterManager tester) {
		super(name, start, end, depth);
		this.tester=tester;
	}
	
	public BasicFilteredPathFinder(String name, NodeFilterManager start, 
			NodeFilterManager end, int depth, boolean stopAtFirstEndpoint, NodeFilterManager tester) {
		super(name, start, end, depth, stopAtFirstEndpoint);
		this.tester=tester;
	}
	
	
	public BasicFilteredPathFinder(String name, NodeFilterManager start, 
			NodeFilterManager end, int depth, boolean stopAtFirstEndpoint, PairDirectory pairs, NodeFilterManager tester) {
		super(name, start, end, depth, stopAtFirstEndpoint, pairs);
		this.tester=tester;
	}

	/**
	 * Outcomes:
	 * 	SAVE_AND_STOP:	Path terminates in endpoint and accepted by filter manager; stopAtFirstEndpoint=true
	 * 	SAVE_AND_CONTINUE: Path terminates in endpoint and accepted by filter manager
	 *  CONTINUE: Depth not yet achieved
	 * 	STOP: Max depth reached without path being verified
	 */
	@Override
	protected PathStatus verify(Path p, int depth) {
		PathStatus plain = super.verify(p, depth);
		
		// if passed the vanilla pathfinder, check on the filter manager.
		if (plain==PathStatus.SAVE_AND_STOP || plain==PathStatus.SAVE_AND_CONTINUE) {
			if (filterAccept(p)) {
				// OK!
				return plain;
			} else {
				// Nope. :*(
				return PathStatus.STOP;
			}
		} else {
			// stop or continue
			return plain;
		}
	}
	
	/**
	 * Accept if the nodefiltermanager accepts.
	 * @param p
	 * @return
	 */
	protected boolean filterAccept(Path p) {
		return this.tester.accept(p.nodes());		
	}
	
	public String toString() {
		String indir = this.pairs==null? "" : ", Indirectory " + pairs.filename();		
		return String.format("BasicFilteredPathFinder %s start=%s, end=%s, depth=%d, filter=%s%s", 
				this.name, this.start.name(), this.end.name(), this.depth, this.tester.name(), indir);
	}
	
	/**
	 * Reads a pathfinder given a line and config.
	 * 'start', 'end', and 'contains' are NodeFilterManagers.
	 * 5 is the depth.
	 * 
	 * PATHFINDER	BasicPathFinder	start	end	5	contains
	 * 
	 * @param line
	 * @param config
	 * @return
	 */
	public static BasicFilteredPathFinder readPathFinder(String[] line, Configuration config)
	throws InvalidValueException {
		String err = "";

		if (line.length < 7 || !line[2].equals("BasicFilteredPathFinder")) {
			throw new InvalidValueException("Does not declare a BasicFilteredPathFinder: " + Arrays.toString(line));
		}
		
		String name = line[1];

		NodeFilterManager start, end, contains;
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
		
		contains = config.getNodeFilterManager(line[6]);
		if (end==null) {
			err="Invalid NodeFilterManager: " + line[6];
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
			return new BasicFilteredPathFinder(name, start, end, depth, stopAtFirstEndpoint, contains);
		
		else return new BasicFilteredPathFinder(name, start, end, depth, stopAtFirstEndpoint, pairs, contains);
	}
}
