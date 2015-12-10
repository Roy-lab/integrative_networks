package pathfinders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import structures.BranchyPath;
import structures.Configuration;
import structures.Edge;
import structures.Graph;
import structures.Graph.RType;
import structures.PairDirectory;
import structures.PairDirectory.PartialOrder;
import structures.Path;
import structures.PathManager;
import utilities.DebugTools;
import exceptions.InvalidValueException;
import filters.EdgeFilterManager;

/**
 * The SourceRegTargetPathFinder
 * finds paths between sources and targets in which the PENULTIMATE
 * node is a candidate downstream regulator for the source.
 * The final interaction can be any interaction.
 * 
 * Post-processing:
 * If there are multiple sources in the path, it restricts the target set to
 * the intersection. 
 * 
 * @author chasman
 *
 */
public class SourceRegTargetPathFinder extends PairPathFinder {

	/*
	 * This kind of pathfinder will produce a BranchyPath
	 * when a sets of multiple paths differ ONLY in the last edge.
	 */
	{
		this.collapseMode = CollapseMode.ALL_BUT_LAST;
	}

	/*
	 * Requires that first and penultimate nodes have an ordered pair.
	 * (example: last interaction MUST be between candidate regulator
	 * and target.)
	 */
	protected PairDirectory penultimateFilter;	

	/*
	 * Allow only a limited number of edges with a particular type?
	 * (example: only allow one RNA binding protein -> Expressed Protein edge 
	 */
	protected HashMap<EdgeFilterManager, Integer> typeLimiter;

	public SourceRegTargetPathFinder(String name, 
			PairDirectory stPairs, PairDirectory candRegs, int depth) {
		super(name, stPairs, depth);
		this.penultimateFilter = candRegs;
	}

	public SourceRegTargetPathFinder(String name, 
			PairDirectory stPairs, PairDirectory candRegs, int depth, int maxDepth, double cov) {
		super(name, stPairs, depth, maxDepth, cov);
		this.penultimateFilter = candRegs;
	}

	public String toString() {	
		return String.format("SourceRegTargetPathFinder %s: source target pairs=%s, source reg pairs=%s, start depth=%d, max depth=%d, reg_coverage>=%f", 
				this.name, this.stPairs.filename(), this.penultimateFilter.filename(), this.depth, this.maxDepth, this.stop);
	}

	/**
	 * Stops when "stop" fraction of penultimate nodes (candidate regulators) has been surpassed, or maximum depth.
	 * So: if fraction=0.5, we need > 50% regulators included.
	 * Prints summary at end of depth.
	 */
	public PathManager findPathsIterative(Graph g) {
		PathManager found = new PathManager();

		// stop if depth==0
		if (this.depth==0) return found;

		// get the start nodes
		Set<String> startNodes = stPairs.getFirsts();
		// search for each start node

		// for printing final summary
		System.out.format("Source\tFinalDepth\tRegsUsed\tRegsTotal\tFracRegs\tTargetsUsed\tTargetsTotal\tFracTargets\n");

		for (String node : startNodes) {
			Set<String> targets = stPairs.getSeconds(node);
			// targets in graph?
			HashSet<String> totT = new HashSet<String>(g.nodes());
			totT.retainAll(targets);
			//System.out.format("%s has %d targets in graph \n", node, totT.size());

			// candidate tfs/rbps
			Set<String> cands = penultimateFilter.getSeconds(node);

			HashSet<String> totR = new HashSet<String>();
			// in graph?
			for (String c : cands) {
				int deg = g.degree(node, RType.INCOMING) + g.degree(node, RType.UNDIRECTED);
				if (deg > 0) totR.add(c);
			}

			//System.out.format("%s has %d candidate TFs/RBPs in graph \n", node, totR.size());


			if (!g.contains(node)) {				
				if (DebugTools.DEBUG) System.out.println("Node not in graph: " + node);
				continue;		
			}

			// iterative deepening loop
			double cov=0.0;
			int atDepth=this.depth;

			PathManager npaths=null;
			while (cov <= this.stop && atDepth <= (this.maxDepth)) {
				npaths = this.findPaths(g, node, atDepth);

				if (DebugTools.DEBUG && npaths.size() > 0) {
					System.out.println(String.format("Found %d paths for starting node %s at depth %d.", 
							npaths.size(), node, atDepth));
				}
				// how many targets and TFs/RBPs covered? 

				int tfound=0;
				HashSet<String> foundT = new HashSet<String>();
				for (String t : targets) {
					if (npaths.contains(t)) {
						tfound++;
						foundT.add(t);
					}
				}	
				HashSet<String> used = new HashSet<String>();
				for (Path p : npaths.allPaths()) {
					used.add(p.getNode(-2));
				}


				atDepth++;		

				// Possible TFs/RBPs/targets have incoming edges
				int posR=0, posT=0;
				for (String r : totR) {
					int deg = g.degree(r, RType.INCOMING) + g.degree(r, RType.UNDIRECTED);
					if (deg > 0) {
						posR++;
					}
				}
				for (String t : targets) {
					int deg = g.degree(t, RType.INCOMING) + g.degree(t, RType.UNDIRECTED);
					if (deg > 0) {
						posT++;
					}
				}

				cov= (posR > 0) ? ((double) used.size()) / posR : 0.0;
				double tcov = (posT > 0) ? ((double) tfound) / posT : 0.0;

				//System.out.format("%s\tDepth %d. Covered %f (%d) of %d candidate TFs/RBPs; %f (%d) of %d targets.\n", 
				//		node, (atDepth-1), cov, used.size(), posR, tcov, tfound, posT);

				//System.out.format("* For %s, covered %d/%d targets using %d/%d regulators.\n", 
				//		node, tfound, posT, used.size(), posR);

				//				for (String s : used) {
				//					System.out.format("\t%s\t%s\tregulator\t%d\n", node, s, (atDepth-1));
				//				}
				//				for (String t : foundT) {
				//					System.out.format("\t%s\t%s\ttarget\t%d\n", node, t.replace("_RNA", "D"), (atDepth-1));
				//				}

			}			

			// npaths is null if node not in graph
			if (npaths != null) {
				found.addAll(npaths);
				this.printSummary(g, npaths, node, atDepth-1);
			} else {

			}
		}

		return found;
	}

	/**
	 * Given a PathManager, print out the final summary of sources/targets/regulators covered.
	 * Coverage in terms of regulators/targets that have incoming edges.
	 * @param found
	 */
	public void printSummary(Graph g, PathManager found, String src, int depth) {

		// how many targets and TFs/RBPs covered? 		
		int tfound=0;
		Set<String> targets = this.stPairs.getSeconds(src);
		HashSet<String> foundT = new HashSet<String>();
		for (String t : targets) {
			if (found.contains(t)) {
				tfound++;
				foundT.add(t);
			}
		}	
		// regulators
		HashSet<String> used = new HashSet<String>();
		Set<String> totR=this.penultimateFilter.getSeconds(src);
		Set<Path> spaths = found.getPaths(src);
		if (spaths != null) {
			for (Path p : spaths) {
				used.add(p.getNode(-2));
			}		
			used.retainAll(totR);
		} 

		// Possible TFs/RBPs from original filter (may not have incoming edges in background network)			
		int posR=0, posT=0;
		for (String r : totR) {
			int deg = g.degree(r, RType.INCOMING) + g.degree(r, RType.UNDIRECTED);
			if (deg > 0) {
				posR++;
			}
		}
		for (String t : targets) {
			int deg = g.degree(t, RType.INCOMING) + g.degree(t, RType.UNDIRECTED);
			if (deg > 0) {
				posT++;
			}
		}

		double cov= (posR > 0) ? ((double) used.size()) / posR : 0.0;
		double tcov = (posT > 0) ? ((double) tfound) / posT : 0.0;

		System.out.format("%s\t%d\t%d\t%d\t%f\t%d\t%d\t%f\n", 
				src, depth, used.size(), posR, cov, tfound, posT, tcov);

	}

	/**
	 * If multiple sources appear in a path, then the set of targets at the end will
	 * be the intersection of all of those sources' targets.
	 */
	@Override
	protected PathManager applyPostProcessing(PathManager found) {
		PathManager newPM=new PathManager();
		for (String label : found.allLabels()) {
			for (Path p : found.getPathsForLabel(label)) {
				Path newPath = this.postProcessPath(p);
				if (newPath != null) {
					newPM.add(newPath, label);
				}
			}
		}
		return newPM;
	}

	/**
	 * Given a path, checks for other sources along the way.
	 * If sources exist, restrict the target set to the intersection.
	 * @param p restricted path, or null if no targets remain.
	 * @return
	 */
	protected Path postProcessPath(Path p) {
		// does it contain other sources? If not, just return the original path
		Set<String> sources=this.stPairs.getFirsts();
		HashSet<String> nodes=new HashSet<String>(p.nodes());
		nodes.retainAll(sources);

		// if only one or 0? sources, no problem; return path.
		if (nodes.size()<=1) {
			return p;
		}

		HashSet<String> targets=new HashSet<String>(p.termini());
		// restrict targets by sources
		for (String s : nodes) {
			//System.out.println(s);
			Set<String> otherTars=this.stPairs.getSeconds(s);
			targets.retainAll(otherTars);
		}

		if (targets.size()==p.termini().size()) {
			//OK - nothing lost
			return p;
		} else if (targets.size()==0) {
			// all lost
			return null;
		} else {		
			// if branchy and we need to restrict
			assert(p instanceof BranchyPath) : "We should only get here with branchy paths";

			BranchyPath bp = (BranchyPath) p;

			// get body
			Path body = bp.bodyPath();
			String pen = body.getNode(-1);

			// get terminal edges
			HashSet<Edge> termE=new HashSet<Edge>();
			for (Edge e : bp.terminalEdges()) {		
				// check for last node - could potentially be either i or j.
				if (targets.contains(e.j()) && e.i().equals(pen)) {
					termE.add(e);
				} else if (targets.contains(e.i()) && e.j().equals(pen)) {
					termE.add(e);
				} else {
					continue;
				}
			}

			Path newPath = new BranchyPath(body, targets, termE);
			return newPath;
		}

	}

	/**
	 * Path ends when we reach one of the source's targets
	 * VIA an approved candidate regulator for the source,
	 * or we run out of depth. 
	 * 
	 */
	@Override
	protected PathStatus verify(Path p, int depth) {

		// does it pass the super-class? (ie, are first and last in a pair?)
		PathStatus pairVerify = super.verify(p, depth);
		if (pairVerify == PathStatus.SAVE_AND_STOP) {			
			// are first and penultimate nodes a candidate source-regulator pair?
			String first = p.getNode(0);
			String penultimate = p.getNode(-2);
			// accept if source-reg pair -- the second test is for cases
			// where the source IS the reg
			boolean isRegPair = 
					(this.penultimateFilter.getOrder(first, penultimate)==PartialOrder.ABOVE)
					|| (first.equals(penultimate) && this.penultimateFilter.hasSelfPair(first));

			// yes! save and stop.
			if (isRegPair) return PathStatus.SAVE_AND_STOP;
			else return PathStatus.STOP;
		}

		// ran out of depth - stop.
		if (depth==0) return PathStatus.STOP;

		// keep going otherwise.
		return PathStatus.CONTINUE;		
	}


	/**
	 * Reads a pairpathfinder given a line and config.
	 * It takes a PairIndex object to designate start and endpoints.
	 * 5 is the depth.
	 * 
	 * PATHFINDER	name	SourceRegTargetPairPathFinder	sourceTarget	sourceReg	5 
	 * 
	 * Iterative option: Start with one depth, go up to a max depth, or stop when 
	 * regulator coverage is >= 0.50.
	 * PATHFINDER	name	SourceRegTargetPairPathFinder	sourceTarget	sourceReg	3	5	0.5
	 * 
	 * @param line
	 * @param config
	 * @return
	 */
	public static PairPathFinder readPathFinder(String[] line, Configuration config)
			throws InvalidValueException {
		String err = "";

		if (line.length < 6 || !line[2].equals("SourceRegTargetPathFinder")) {
			throw new InvalidValueException("Does not declare a SourceRegTargetPathFinder: " + Arrays.toString(line));
		}

		String name = line[1];

		PairDirectory pairs, regs;
		int depth=0;		

		pairs = config.getPairDirectory(line[3]);
		if (pairs==null) {
			err="Invalid source-target PairDirectory: " + line[3];
		}

		regs = config.getPairDirectory(line[4]);
		if (regs==null) {
			err="Invalid source-regulator PairDirectory: " + line[4];
		}

		try {
			depth = Integer.parseInt(line[5]);	
		} catch (NumberFormatException nfe) {
			err="Invalid depth:" + line[5];
		}		

		SourceRegTargetPathFinder finder=null; 
		if (line.length > 6) {
			int maxDepth=depth;
			double cov=0.0;
			try {
				maxDepth=Integer.parseInt(line[6]);
				cov=Double.parseDouble(line[7]);

			} catch (NumberFormatException nfe) {
				err=String.format("Invalid max depth (%s) or coverage (%s) specified in config.", line[6], line[7]);
			}
			finder=new SourceRegTargetPathFinder(name, pairs, regs, depth, maxDepth, cov);
		} else {			
			finder=new SourceRegTargetPathFinder(name, pairs, regs, depth);

		}

		if (err.length() > 0 ) {
			throw new InvalidValueException(err);
		}

		return finder;
	}

}
