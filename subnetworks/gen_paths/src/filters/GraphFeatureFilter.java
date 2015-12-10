package filters;

import structures.Graph;

/**
 * Lets us make filters based off graph features.
 * T is either node or edge.
 * @author chasman
 *
 * @param <T>
 */
public interface GraphFeatureFilter<T> {

	public boolean accept(T item, Graph g);
	
}
