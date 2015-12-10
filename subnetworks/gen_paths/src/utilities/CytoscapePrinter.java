package utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import structures.BranchyPath;
import structures.Edge;
import structures.EdgeLibrary;
import structures.Feature;
import structures.Graph;
import structures.NodeLibrary;
import structures.Path;
import structures.PathManager;
import structures.Value;

/**
 * Prints things out in Cytoscape-friendly format.
 * @author chasman
 *
 */
public class CytoscapePrinter {

	public static final String NONE="";
	public static final String DELIM="\t";


	/**
	 * Prints out a graph to a sif-format file.
	 * Super general for now - just directed and undirected edges.
	 * a	(type)	b
	 * At this point, we don't duplicate undirected edges...
	 * @param g
	 * @param output
	 * @param cleanUp	if true, strips non-alphanumeric chars from node names
	 * @return	true if file written, false if file not.
	 */
	public static boolean printSif(Graph g, String output, boolean cleanUp) {
		File outf = null;
		PrintWriter pw = null;

		try {
			outf = new File(output);
			pw = new PrintWriter(outf);

			// write each edge
			ArrayList<Edge> edges = new ArrayList<Edge>(g.edges());
			Collections.sort(edges);

			for (Edge e : edges) {
				String sif = edgeSif(e, false, cleanUp); // no parens
				pw.write(String.format("%s\n", sif));
			}

		} catch (FileNotFoundException fnfe) {
			// can't write for whatever reason
			return false;
		} finally {
			if (pw != null) pw.close();
		}			

		return true;
	}

	public static boolean printSif(Graph g, String output) {
		return printSif(g, output, false);
	}

	/**
	 * Prints a sif from a PathManager. For undirected edges, we will only
	 * print in the direction(s) used.
	 * @param pm	path manager containing edges of interest
	 * @param output	output filename
	 * @param cleanUp	if true, removes non-alpha-numeric chars from node names
	 * @return
	 */
	public static boolean printSif(PathManager pm, String output, boolean cleanUp) {
		// container to hold edges as we make them
		HashSet<String> edges = new HashSet<String>();

		// go through each path
		for (Path p : pm.allPaths()) {
			List<String> pathEdges=edgeSifs(p, cleanUp);
			for (String s : pathEdges) {
				edges.add(s);
			}
//			// aaand each edge
//			for (int i = 0; i <  p.edgeLength(); i++) {
//				Edge e = p.getEdge(i);
//				if (e.isDirected()) {
//					edges.add(edgeSif(e,false, cleanUp));
//				} else {
//					// what's the direction here?
//					// forward
//					if (e.i().equals(p.getNode(i))) {
//						edges.add(edgeSif(e,false, cleanUp));
//					} 
//					// reverse
//					else {
//						assert(e.j().equals(p.getNode(i))): "Trying to print a badly formed path: " + p.toString();
//						edges.add(edgeSifReverse(e,false, cleanUp));
//					}
//				}
//			}
		}

		File outf = null;
		PrintWriter pw = null;

		try {
			outf = new File(output);
			pw = new PrintWriter(outf);

			// write each edge			
			for (String sif : edges) {
				pw.write(String.format("%s\n", sif));
			}

		} catch (FileNotFoundException fnfe) {
			// can't write for whatever reason
			return false;
		} finally {
			if (pw != null) pw.close();
		}			

		return true;
	}

	public static boolean printSif(PathManager pm, String output) {
		return printSif(pm, output, false);
	}

	/**
	 * Gets ordered list of sif-format edges from this path,
	 * oriented for the path.
	 * @param p
	 * @param useParens
	 * @param doClean
	 * @return
	 */
	public static List<String> edgeSifs(Path p, boolean cleanUp) {
		ArrayList<String> edges = new ArrayList<String>();
			
		// do all except last edge
		for (int i = 0; i <  p.edgeLength()-1; i++) {
			Edge e = p.getEdge(i);
			if (e.isDirected()) {
				edges.add(edgeSif(e,false, cleanUp));
			} else {
				// what's the direction here?
				// forward
				if (e.i().equals(p.getNode(i))) {
					edges.add(edgeSif(e,false, cleanUp));
				} 
				// reverse
				else {
					assert(e.j().equals(p.getNode(i))): "Trying to print a badly formed path: " + p.toString();
					edges.add(edgeSifReverse(e,false, cleanUp));
				}
			}
		}
		
		String penultimateNode=p.getNode(-2);
		// do termini - will just be last edge if not branchy.
		for (Edge e : p.terminalEdges()) {
			if (e.isDirected()) {
				edges.add(edgeSif(e,false, cleanUp));
			} else {
				// what's the direction here?
				// forward
				if (e.i().equals(penultimateNode)) {
					edges.add(edgeSif(e,false, cleanUp));
				} 
				// reverse
				else {
					assert(e.j().equals(penultimateNode)): "Trying to print a badly formed path: " + p.toString();
					edges.add(edgeSifReverse(e,false, cleanUp));
				}
			}
		}
		
		
		return edges; //return StringUtils.join(edges, delimiter);
	}

	/**
	 * Gets the sif format for a string.
	 * a (u) b
	 * or a (d) b
	 * @param e
	 * @return
	 */
	protected static String edgeSif(Edge e, boolean useParens, boolean doClean) {
		String type = e.isDirected() ? "d" : "u";
		if (useParens) {
			type = String.format("(%s)", type);
		}
		String i=e.i();
		String j=e.j();
		if (doClean) {
			i=clean(i);
			j=clean(j);
		}

		return String.format("%s %s %s", i, type, j);
	}
	protected static String edgeSif(Edge e, boolean useParens) {
		return edgeSif(e, useParens, false);
	}

	/**
	 * Cleans non alphanumeric characters from string
	 * @param s
	 * @return
	 */
	public static String clean(String s) {
		return s.replaceAll("[^A-Za-z0-9]", ""); 
	}

	/**
	 * Gets the sif format for a string.
	 * Prints nodes in reverse order. You shouldn't really use this on directed edges.
	 * a (u) b
	 * @param e	edge
	 * @param useParens	if true, encase interaction type in parens (for edge attributes in Cytoscape 2.x)
	 * @return
	 */
	protected static String edgeSifReverse(Edge e, boolean useParens, boolean doClean) {
		assert(!e.isDirected()) : "Are you sure you want to print your directed edge in reverse?";

		String type = "u"; 
		if (useParens) {
			type = String.format("(%s)", type);
		}

		String i=e.i();
		String j=e.j();
		if (doClean) {
			i=clean(i);
			j=clean(j);
		}
		return String.format("%s %s %s", j, type, i);
	}
	protected static String edgeSifReverse(Edge e, boolean useParens) {
		return edgeSifReverse(e, useParens, false);
	}

	/**
	 * Prints out an edge attribute with string value.
	 * For undirected edges, attach same gams ID to both directions.
	 * @param attrName	name of feature
	 * @param idMap	map from edgeID to value
	 * @param filename	output filename
	 * @param cytoV3	if true, print a tab-delimited file for Cytoscape v3+; 
	 * 					otherwise, print attribute for Cytoscape v<3
	 * @param cleanUp	if true, strip non alpha-numeric chars from node names in edge sif
	 * @return
	 */
	public static boolean printEdgeAttribute(String attrName, Map<Edge, String> idMap, String filename, boolean cytoV3, boolean cleanUp) {

		File outf = null;
		PrintWriter pw = null;

		try {
			outf = new File(filename);
			pw = new PrintWriter(outf);					

			if (cytoV3) pw.format("ID\t%s\n", attrName);
			else pw.format("%s\n", attrName);

			for (Edge e : idMap.keySet()) {
				if (cytoV3) {
					pw.format("%s\t%s\n", edgeSif(e, true, cleanUp), idMap.get(e));
					// duplicate for both directions of undirected edge
					if (!e.isDirected()) {
						pw.format("%s\t%s\n", edgeSifReverse(e, true, cleanUp), idMap.get(e));
					}
				}
				else {
					pw.format("%s = %s\n", edgeSif(e, true, cleanUp), idMap.get(e));
					// duplicate for both directions of undirected edge
					if (!e.isDirected()) {
						pw.format("%s = %s\n", edgeSifReverse(e, true, cleanUp), idMap.get(e));
					}
				}
			}

		} catch (FileNotFoundException fnfe) {
			// can't write for whatever reason
			return false;
		} finally {
			if (pw != null) pw.close();
		}				

		return true;
	}


	/**
	 * Prints out all node features to a table for Cytoscape 3 node attribute
	 * reading. 
	 * @param libe
	 * @param output file
	 * @return
	 */
	public static boolean printNodeFeatures(NodeLibrary libe, String output, boolean cleanUp) {

		List<Feature> feats = new ArrayList<Feature>(libe.features());
		if (feats.size() == 0) {
			// no features to print
			return false;
		}

		Collections.sort(feats, new Comparator<Feature>() {
			public int compare(Feature a, Feature b) {
				return a.name().compareTo(b.name());
			}
		});

		File outf = null;
		PrintWriter pw = null;

		try {
			outf = new File(output);
			pw = new PrintWriter(outf);

			// print the feature names
			pw.write("NODE");
			for (Feature f : feats) {
				pw.write(DELIM + f.name() );
			}
			pw.write("\n");

			// for each node, for each feature
			for (String nodeO : libe.items()) {
				String node=nodeO;
				if (cleanUp) {
					node=clean(nodeO);
				}
				pw.write(node);
				for (Feature f : feats) {
					Value val = libe.getValue(node, f);
					String valStr = val != null ? val.toString() : NONE; 
					pw.write(String.format("%s%s", DELIM, valStr));
				}
				pw.write("\n");
			}

		} catch (FileNotFoundException fnfe) {
			// can't write for whatever reason
			return false;
		} finally {
			if (pw != null) pw.close();
		}				

		return true;
	}

	/**
	 * Prints edge features to tab-delim file.
	 * Prints undirected edges in both directions.
	 * 
	 * @param libe
	 * @param output
	 * @param cleanUp	if true, removes non-alphanumeric chars from node names
	 * @return
	 */
	public static boolean printEdgeFeatures(EdgeLibrary libe, String output, boolean cleanUp) {

		List<Feature> feats = new ArrayList<Feature>(libe.features());
		if (feats.size() == 0) {
			// no features to print
			return false;
		}

		Collections.sort(feats, new Comparator<Feature>() {
			public int compare(Feature a, Feature b) {
				return a.name().compareTo(b.name());
			}
		});

		File outf = null;
		PrintWriter pw = null;

		try {
			outf = new File(output);
			pw = new PrintWriter(outf);

			// for each edge, print the sif-style edge
			// a type c
			// NO LONGER: along with the unique id, a.b.d/a.b.u.
			pw.write(String.format("sif_style"));			

			// print the feature names
			for (Feature f : feats) {
				pw.write(DELIM + f.name() );
			}
			
			// unbounded feature names
			for (String s : libe.getUnboundFeatureNames()) {
				pw.write(DELIM + s);
			}						
			
			pw.write(DELIM + "filenames");		
			
			pw.write("\n");


			//DELIM + "filename(s)\n");

			// for each edge, for each feature
			for (Edge item : libe.items()) {
				StringBuilder row = new StringBuilder();

				boolean first=true;
				
				// key
				// row.append(item.toString());
				for (Feature f : feats) {
					Value val = libe.getValue(item, f);
					String valStr = val != null ? val.toString() : NONE; 
					// first one? no delimiter.
					if (first) {
						row.append(String.format("%s", valStr));
						first=false;
					} else {
						row.append(String.format("%s%s", DELIM, valStr));
					}
				}
				
				// unbound features				
 				Map<String,String> vals = libe.getUnboundFeatures(item);
// 				if (vals == null) {
// 					System.out.println(item);
// 					System.out.println(libe.getFilenames(item));
// 				}
				for (String u : libe.getUnboundFeatureNames()) {					
					if (vals != null && vals.containsKey(u)) {
						row.append(String.format("%s%s", DELIM, vals.get(u)));
					} else {
						row.append(String.format("%s", DELIM));
					}
				}
				

				// keep only base filename, not extension or path
				ArrayList<String> strs = new ArrayList<String>();
				for (String fn : libe.getFilenames(item)) {
					// remove path
					String basename = new File(fn).getName();

					// remove extension
					String[] sp = basename.split("\\.(?=[^\\.]+$)");
					strs.add(sp[0]);
				}
				row.append(String.format("%s%s", DELIM, StringUtils.join(strs,  "|")));
				
				row.append("\n");

				// siffy comes first, but we do it last so we can make two.
				String sif = edgeSif(item, true, cleanUp); //item.isDirected() ? "d" : "u";	
				pw.write( String.format("%s%s%s", sif, DELIM, row.toString()));

				if (!item.isDirected()) {
					pw.write( String.format("%s%s%s", edgeSifReverse(item, true, cleanUp), DELIM, row.toString()));
				}
			}			


		} catch (FileNotFoundException fnfe) {
			// can't write for whatever reason
			return false;
		} finally {
			if (pw != null) pw.close();
		}			
		return true;
	}
}
