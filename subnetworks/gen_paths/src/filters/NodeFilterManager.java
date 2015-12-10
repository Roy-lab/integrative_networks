package filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import structures.Configuration;
import structures.Feature;
import structures.Graph;
import structures.NodeLibrary;
import structures.Value;
import utilities.StringUtils;
import exceptions.InvalidValueException;

/**
 * Contains one or more filters and their running modes.
 * @author chasman
 *
 */
public class NodeFilterManager extends FilterManager<String, Filter, NodeLibrary>{
			
	public NodeFilterManager(String name, Collection<Filter> filters,
			FilterItemMode itemMode, FilterSetMode setMode, NodeLibrary library) {
		super(name, filters, itemMode, setMode, library);
	}
	
	public NodeFilterManager(String name, Filter filter,
			FilterItemMode itemMode, FilterSetMode setMode, NodeLibrary library) {
		super(name, filter, itemMode, setMode, library);
	}
	
	/**
	 * Gets a feature value for a node. 
	 * @param node
	 * @return
	 */
	@Override
	protected Value getValue(String node, Feature f) {
		return this.library.getValue(node, f);
	}
	

	/**
	 * Tests each node in a graph individually for acceptance by the
	 * NodeFilterManager. Returns the subgraph consisting of nodes
	 * that are accepted.
	 * @param orig
	 * @param manager
	 * @return
	 */
	public Graph filter(Graph orig) {
		HashSet<String> keepNodes = new HashSet<String>();
		for (String n : orig.nodes()) {
			if (this.accept(n)) keepNodes.add(n);
		}
		Graph copy = orig.restrict(keepNodes);
		return copy;
	}
	
	/**
	 * Reads a NodeFilterManager from a line, given a Configuration.
	 * 
	 * Example line:
	 * 				  name, itemmode, setmode, filter(s)
	 * NFILTERMANAGER reticulons	or	any	reepticulon|reticulons
	 * @param line
	 * @param config
	 * @throws InvalidValueException	if requested filters don't exist in the config.
	 * @return
	 */
	public static NodeFilterManager readFilterManager(String[] line, Configuration config) 
	throws InvalidValueException {
		if (line.length < 5) {
			throw new InvalidValueException("NodeFilterManager not declared properly: " + Arrays.toString(line));
		}
		String name = line[1];
		FilterItemMode itemMode = FilterItemMode.valueOf(line[2].toUpperCase());
		FilterSetMode setMode = FilterSetMode.valueOf(line[3].toUpperCase());
		
		// get the filters
		ArrayList<Filter> filters = new ArrayList<Filter>();
		String[] fsplit = line[4].split(DELIM);
		for (String fname : fsplit) {
			Filter filt = config.getNodeFilter(fname);
			if (filt==null) {
				throw new InvalidValueException(
						String.format("Filter %s not declared before NodeFilterManager %s.",
					fname, name));
			}
			filters.add(filt);
		}
		
		NodeFilterManager man = new NodeFilterManager(name, filters, 
				itemMode, setMode, config.nodeLibrary()); 
		return man;
	}
	
	public String toString() {
		String report = String.format("NodeFilterManager %s\t%s\t%s\n\t%s", 
				this.name,
				this.itemMode,
				this.setMode,
				StringUtils.sortJoin(this.filters, "\n\t"));
		return report;
	}

	
}
