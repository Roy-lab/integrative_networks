package filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import structures.Configuration;
import structures.Edge;
import structures.EdgeLibrary;
import structures.Feature;
import structures.Graph;
import structures.Value;
import utilities.StringUtils;
import exceptions.InvalidValueException;

public class EdgeFilterManager extends FilterManager<Edge, Filter, EdgeLibrary> {

	public EdgeFilterManager(String name, Collection<Filter> filters,
			FilterItemMode itemMode,
			FilterSetMode setMode, EdgeLibrary library) {
		super(name, filters, itemMode, setMode, library);
	}

	@Override
	protected Value getValue(Edge item, Feature f) {
		return this.library.getValue(item, f);
	}

	/**
	 * Tests each edge for acceptance. Returns
	 * the subgraph consisting of accepted edges.
	 */
	@Override
	public Graph filter(Graph g) {
		Graph newG = new Graph();
		for (Edge e : g.edges()) {
			if (this.accept(e)) {
				newG.add(e);
			}
		}
		return newG;
	}
	
	/**
	 * Reads an EdgeFilterManager from a line, given a Configuration.
	 * 
	 * Example line:
	 * 				  name, itemmode, setmode, filter(s)
	 * EFILTERMANAGER reticulons	or	any	reepticulon|reticulons
	 * @param line
	 * @param config
	 * @throws InvalidValueException	if requested filters don't exist in the config.
	 * @return
	 */
	public static EdgeFilterManager readFilterManager(String[] line, Configuration config) 
	throws InvalidValueException {
		if (line.length < 5) {
			throw new InvalidValueException("EdgeFilterManager not declared properly: " + Arrays.toString(line));
		}
		String name = line[1];
		FilterItemMode itemMode = FilterItemMode.valueOf(line[2].toUpperCase());
		FilterSetMode setMode = FilterSetMode.valueOf(line[3].toUpperCase());
		
		// get the filters
		ArrayList<Filter> filters = new ArrayList<Filter>();
		String[] fsplit = line[4].split(DELIM);
		for (String fname : fsplit) {
			Filter filt = config.getEdgeFilter(fname);
			if (filt==null) {
				throw new InvalidValueException(
						String.format("Filter %s not declared before EdgeFilterManager %s.",
					fname, name));
			}
			filters.add(filt);
		}
		
		EdgeFilterManager man = new EdgeFilterManager(name, filters, 
				itemMode, setMode, config.edgeLibrary()); 
		return man;
	}
	
	public String toString() {
		String report = String.format("EdgeFilterManager %s\t%s\t%s\n\t%s", 
				this.name,
				this.itemMode,
				this.setMode,
				StringUtils.sortJoin(this.filters, "\n\t"));
		return report;
	}
	

}
