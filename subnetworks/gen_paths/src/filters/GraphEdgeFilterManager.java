package filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import structures.Configuration;
import structures.Edge;
import structures.Feature;
import structures.Graph;
import structures.Value;
import utilities.GraphUtils;
import utilities.StringUtils;
import exceptions.InvalidValueException;

public class GraphEdgeFilterManager extends FilterManager<Edge, Filter, Graph> {
	public GraphEdgeFilterManager(String name, Filter filter,
			FilterItemMode itemMode, FilterSetMode setMode, Graph g) {
		super(name, filter, itemMode, setMode, g);
	}

	public GraphEdgeFilterManager(String name, Collection<Filter> filters,
			FilterItemMode itemMode, FilterSetMode setMode, Graph g) {
		super(name, filters, itemMode, setMode, g);
	}

	@Override
	protected Value getValue(Edge item, Feature f) {
		return GraphUtils.getValue(item, f, this.library);
	}

	public String toString() {
		String report = String.format("GraphEdgeFilterManager %s\t%s\t%s\n\t%s", 
				this.name,
				this.itemMode,
				this.setMode,
				StringUtils.sortJoin(this.filters, "\n\t"));
		return report;
	}


	/**
	 * Reads a GraphFilterManager from a line, given a Configuration.
	 * Initialize with a null graph.
	 * 
	 * Example line:
	 * 				  name, itemmode, setmode, filter(s)
	 * GFILTERMANAGER degreez	or	any	degree_f
	 * @param line
	 * @param config
	 * @throws InvalidValueException	if requested filters don't exist in the config.
	 * @return
	 */
	public static GraphEdgeFilterManager readFilterManager(String[] line, Configuration config) 
	throws InvalidValueException {
		if (line.length < 5) {
			throw new InvalidValueException("GraphFilterManager not declared properly: " + Arrays.toString(line));
		}
		String name = line[1];
		FilterItemMode itemMode = FilterItemMode.valueOf(line[2].toUpperCase());
		FilterSetMode setMode = FilterSetMode.valueOf(line[3].toUpperCase());

		// get the filters
		ArrayList<Filter> filters = new ArrayList<Filter>();
		String[] fsplit = line[4].split(DELIM);
		for (String fname : fsplit) {
			// accepts both node and graphnode filters
			Filter filt = config.getGraphFilter(fname);
			if (filt==null) {
				throw new InvalidValueException(
						String.format("GraphFilter %s not declared before GraphFilterManager %s.",
								fname, name));
			}
			filters.add(filt);
		}

		// initialize with null graph
		GraphEdgeFilterManager man = new GraphEdgeFilterManager(name, filters, 
				itemMode, setMode, null); 
		return man;
	}

	/**
	 * Apply this FilterManager to a graph.
	 * Returns the nodes that (individually) pass the filter.
	 * Sets this manager's library to the given graph.
	 */
	@Override
	public Graph filter(Graph g) {
		this.setLibrary(g);
		HashSet<Edge> keepEdges = new HashSet<Edge>();
		for (Edge e : g.edges()) {
			if (this.accept(e)) keepEdges.add(e);
		}
		Graph copy = new Graph(keepEdges);
		return copy;
	}	

}
