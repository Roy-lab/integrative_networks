package apps;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import pathfinders.PairPathFinder;
import pathfinders.PathFinder;
import structures.Configuration;
import structures.Graph;
import structures.Path;
import structures.PathManager;
import utilities.CytoscapePrinter;
import utilities.GamsPrinter;
import utilities.StringUtils;
import utilities.GamsPrinter.LabelMode;

/**
 * Prepared to demonstrate how to use this code to find paths!
 * First argument is the config filename.
 * 
 * Usage:
 * java	SearchTester config_filename
 * 
 * This application will:
 * -Read in a config file for a  
 * -
 * @author chasman
 *
 */
public class SearchTester {
	
	public static void main(String[] args) {
		
		Configuration config = null;
		Graph g = null;
		
		try {
			config = Configuration.readConfigFile(args[0]);
			g = config.buildGraph();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return;
		}
		
		// print summary of edges
		System.out.format("Read %d edges.\n%s\n", config.edgeLibrary().size(), config.edgeLibrary().toString());
		
		// Print summary of nodes
		System.out.println(config.nodeLibrary().toString());
			
		// Run through the pathfinders
		ArrayList<PathFinder> pfs = config.pathFinders();

		// Save all the paths into this PathManager
		PathManager paths = new PathManager();
		try {
			for (PathFinder pf : pfs) {
				PathManager found = null;
				if (pf instanceof PairPathFinder) {
					// iterative deepening
					found = pf.findPathsIterative(g, 2, 0.50);
					
				}
				else {
					// no iterative deepening
					found = pf.findPaths(g);	
				}
				
				System.out.format("Applied pathfinder %s: found %d paths\n", pf.toString(), found.size());
				//for (Path p : found.allPaths()) {
				//	System.out.println("\t" + p);
				//}
				
				if (found != null) paths.addAll(found);
			}
			System.out.format("Total found: %d paths\n", paths.size());

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
		// Print all the paths with their pathfinders
		//for (Path p : paths.allPaths()) {
		//	System.out.format("\t%s\t%s\n", p, paths.getLabels(p));
		//}
				
		// Print GAMS and Cytoscape output--------------------------------------
		// Cytoscape printing - do I strip non-alphanumeric characters from node names?
		boolean cleanMode=(config.getGamsLabelMode()==LabelMode.STRIP);

		// Make a graph consisting of only the paths.
		Graph gPathsOnly = PathManager.makeGraph(paths); 		
		GamsPrinter printer = new GamsPrinter(g, g, paths, config.nodeLibrary(), 
				config.edgeLibrary(), config.subgraphs(), config.pairDirectories());
	
		
		// Just paths
		CytoscapePrinter.printSif(gPathsOnly, String.format("%s_paths.sif", config.getOutputPrefix()), cleanMode);
		
		// Entire input network
		CytoscapePrinter.printSif(g, String.format("%s_background.sif", config.getOutputPrefix()), cleanMode);
		
		// print all node and edge features into tables for import into Cytoscape
		CytoscapePrinter.printNodeFeatures(config.nodeLibrary(), String.format("%s_node_feats.tab", config.getOutputPrefix()), cleanMode);
		CytoscapePrinter.printEdgeFeatures(config.edgeLibrary(), String.format("%s_edge_feats.tab", config.getOutputPrefix()), cleanMode);

		System.out.println("Wrote Cytoscape files: " + config.getOutputPrefix());

		// print an Edge Attribute representing edge IDs that we assigned 
		// for the GAMS representation of the edges
		// These IDs are created during GAMS printing.
		boolean cytoV3Format=true;	// Should attribute be tab-delimited file
								// for Cytoscape v.3+, or formatted for
								// Cytoscape v.<3?		
		CytoscapePrinter.printEdgeAttribute("gamsId", printer.getEdgeIDs(), 
				String.format("%s_gamsId.edge", config.getOutputPrefix()), cytoV3Format, cleanMode);

		// Print GAMS file to file (if specified in config) or console otherwise
		String gamsPref = config.getGamsFileName();
		String gamsFn = String.format("%s.gms", gamsPref);
		if (gamsFn != null) {
			PrintStream gamsStream = null;
			if (gamsFn.equals("System.out")) {
				gamsStream = System.out;
			} else {
				try {
					gamsStream = new PrintStream(new File(gamsFn));
				} catch (IOException ioe) {
					System.out.println("Unable to print gams file to " + gamsFn);
				}
			}

			// Print node features requested in config file
			// (or print all of them)
			String[] nfeats = config.getGamsNodeFeatureNames();
			if (nfeats != null)	printer.printNodeSets(gamsStream, nfeats);
			else printer.printNodeSets(gamsStream);
			
			// Print edge features requested in config file
			// (or print all of them)
			String[] efeats = config.getGamsEdgeFeatureNames();
			if (efeats != null)	printer.printEdgeSets(gamsStream, efeats, true);
			else printer.printEdgeSets(gamsStream);
			
			//pairs
			printer.printPairSets(gamsStream, true);
			
			// Print sets describing the paths			
			printer.printPathSets(gamsStream);
		}
		System.out.println("Wrote GAMS file to " + gamsFn);
		
		String pathFn = String.format("%s_paths.tab", gamsPref);
		PrintStream pathStream = null;
		try {
			pathStream = new PrintStream(new File(pathFn));
			System.out.println("Opened new path file " + pathFn);
			printPathAssociationFile(config, paths, printer, pathStream);
		} catch (IOException ioe) {
			System.err.println("Unable to print paths to " + pathFn);
		}
		finally {
			pathStream.close();
		}

		System.out.format("Printed path strings to %s\n", pathFn);
	}
	
	/**
	 * Prints a text file containing ordered nodes/edges for each path 
	 * @param config
	 * @param paths
	 * @param printer
	 * @param outStream
	 */
	protected static void printPathAssociationFile(Configuration config, PathManager paths,
			GamsPrinter printer, PrintStream outStream) {
		outStream.format("#pid\tgene_ids\teids\tsubgraphs\n");
		for (Path p : paths.allPaths()) {
			String pid = printer.gamsify(p);
			String elist = printer.edgeString(p);
			String nlist = printer.nodeString(p);
			ArrayList<String> slist = printer.gamsifyList(paths.getLabels(p));
			outStream.format("%s\t%s\t%s\t%s\n", pid,  
					nlist, elist, StringUtils.join(slist,"|"));
		}
	}

}
