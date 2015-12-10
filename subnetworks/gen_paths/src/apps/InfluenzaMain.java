package apps;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

import pathfinders.PathFinder;
import structures.Configuration;
import structures.Edge;
import structures.EdgeLibrary;
import structures.Graph;
import structures.NodeLibrary;
import structures.Path;
import structures.PathManager;
import structures.Subgraph;
import utilities.CytoscapePrinter;
import utilities.GamsPrinter;
import utilities.GamsPrinter.LabelMode;
import utilities.StringUtils;

/**
 * Given a directory of files containing lists of held-aside hits, 
 * produce a directory of reduced GAMS set files.
 * Reads config filename from args[0]
 * @author chasman
 *
 */
public class InfluenzaMain {

	public static final boolean DO_PATHFINDING=true;

	public static void main(String[] args) {

		Configuration config = null;
		Graph g=null, gOrig = null;
		try {
			config = Configuration.readConfigFile(args[0]);
			g = config.buildGraph();
			gOrig= config.buildGraph(); ///Graph.createFromEdgeLibrary(config.edgeLibrary());
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return;
		}

		// check GAMS prefix
		String gamsPref = config.getGamsFileName();
		if (gamsPref == null || gamsPref.equals("System.out")) {
			System.err.println("Sorry, stdout is not an option for the sampler. Please specify a gams prefix.");
			return;
		}

		// remove self loops and edgeless nodes
		gOrig = gOrig.removeSelfLoops();
		gOrig = gOrig.removeEdgeless();

		g = g.removeEdgeless();
		g = g.removeSelfLoops();
		

		NodeLibrary libe = config.nodeLibrary();
		EdgeLibrary elibe = config.edgeLibrary();
		
		
		System.out.println(libe.toString());
		System.out.println(elibe.toString());
		
		System.out.format("Read graph with %d nodes and %d edges.\n", gOrig.nodes().size(), gOrig.edges().size());
		System.out.format("Auto-filtered to graph with %d nodes and %d edges.\n", g.nodes().size(), g.edges().size());
		
		System.out.println("Summary of background network:");
		// summarize features in background network
		System.out.println(libe.summarize(g.nodes()));
		System.out.println(elibe.summarize(g.edges()));

		// run all pathfinders
		ArrayList<PathFinder> pfs = config.pathFinders();

		PathManager paths = new PathManager();
		// find paths?
		if (DO_PATHFINDING) {
			try {
				for (PathFinder pf : pfs) {
					PathManager found = pf.findPaths(g);
					//PathManager found = pf.findPathsIterative(g, 4, 0.75);
					System.out.format("Applied %s: %d paths\n", pf.toString(), found.size());
					paths.addAll(found);
				}
				System.out.format("Total: %d paths\n", paths.size());

			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}

			g = PathManager.makeGraph(paths);
			// add the subgraphs
			for (Subgraph sg : config.subgraphs().values()) {
				// screen edges
				Collection<Edge> edges = 
						PathManager.filterEdges(paths, sg.edges(), 
								config.subgraphAddModes().get(sg.name()));
				g.addAll(edges);
			}
			
			System.out.format("Paths and subgraphs contain %d nodes and %d edges.\n", g.nodes().size(), g.edges().size());

			// summarize features
			System.out.println(libe.summarize(g.nodes()));
			System.out.println(elibe.summarize(g.edges()));
		}


		// cytoscape
		// Cytoscape printing - do I strip non-alphanumeric characters from node names?
		boolean cleanMode=(config.getGamsLabelMode()==LabelMode.STRIP);

		if (DO_PATHFINDING) {
			//CytoscapePrinter.printSif(g, String.format("%s_paths.sif", config.getOutputPrefix()));
			CytoscapePrinter.printSif(paths, String.format("%s_paths.sif", config.getOutputPrefix()), cleanMode);
		}

		CytoscapePrinter.printSif(gOrig, String.format("%s_background.sif", config.getOutputPrefix()), cleanMode);		
		CytoscapePrinter.printNodeFeatures(libe, String.format("%s_node_feats.tab", config.getOutputPrefix()), cleanMode);
		CytoscapePrinter.printEdgeFeatures(config.edgeLibrary(), String.format("%s_edge_feats.tab", config.getOutputPrefix()), cleanMode);

		System.out.println("Wrote Cytoscape files: " + config.getOutputPrefix());

		GamsPrinter printer = new GamsPrinter(g, gOrig, paths, 
				config.nodeLibrary(), config.edgeLibrary(), config.subgraphs(), config.pairDirectories());
		printer.setLabelMode(config.getGamsLabelMode());

		// print unique edge IDs
		boolean cytoV3Format=false;	// Should attribute be tab-delimited file
		// for Cytoscape v.3+, or formatted for
		// Cytoscape v.<3?
		CytoscapePrinter.printEdgeAttribute("gamsId", printer.getEdgeIDs(), 
				String.format("%s_gamsId.edge", config.getOutputPrefix()), cytoV3Format, cleanMode);

		// print a single GAMS file

		String gamsFn = String.format("%s.gms", gamsPref);

		// if it already exists, overwrite
		//File to = new File(gamsFn);
		//if (to.exists()) {
		//System.out.format("Found existing file %s; skipping this job.\n", gamsFn);
		//continue;
		//}		

		// open new output file
		PrintStream gamsStream=null;
		try {
			gamsStream = new PrintStream(new File(gamsFn));
			System.out.println("Opened new gams file " + gamsFn);

			//String[] nfeats = new String[] {""};
			printer.printNodeSets(gamsStream);
			// edge features
			String[] efeats = new String[] {"reg", "etype"};
			printer.printEdgeSets(gamsStream, efeats, false);

			// print paths with directions
			printer.printPathSets(gamsStream, true);
			
			// print pair sets - print all pairs to make it easier for scores for now.
			printer.printPairSets(gamsStream, false);
		} catch (IOException ioe) {
			System.err.println("Unable to print gams file to " + gamsFn);
		} finally {
			gamsStream.close();
		}


		System.out.format("Created GAMS output file %s\n", gamsFn);

		String pathFn = String.format("%s_paths.tab", gamsPref);
		PrintStream pathStream = null;
		try {
			pathStream = new PrintStream(new File(pathFn));
			System.out.println("Opened new path file " + pathFn);
			printPathAssociationFile(config, paths, printer, pathStream, cleanMode);
		} catch (IOException ioe) {
			System.err.println("Unable to print paths to " + pathFn);
		}
		finally {
			pathStream.close();
		}

		System.out.format("Printed path strings to %s\n", pathFn);

	}	

	/**
	 * Prints a text file containing ordered nodes/edges for each path.
	 * @param config
	 * @param paths
	 * @param printer
	 * @param outStream
	 */
	protected static void printPathAssociationFile(Configuration config, PathManager paths,
			GamsPrinter printer, PrintStream outStream, boolean cleanMode) {
		outStream.format("#pid\tgene_ids\teids\tpathfinders\tsif_edges\n");
		for (Path p : paths.allPaths()) {
			String pid = printer.gamsify(p);
			String elist = printer.edgeString(p);
			String nlist = printer.nodeString(p);
			ArrayList<String> slist = printer.gamsifyList(paths.getLabels(p));
			
			List<String> edgeSifs = CytoscapePrinter.edgeSifs(p, cleanMode);
			
			outStream.format("%s\t%s\t%s\t%s\t%s\n", pid,  
					nlist, elist, StringUtils.join(slist,"|"), StringUtils.join(edgeSifs, "|"));
		}
	}
	
	
}
