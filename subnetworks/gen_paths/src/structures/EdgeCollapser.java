package structures;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Collapses edges based on sign/direction information.
 * Each type of EdgeLibraryCollapser knows a different way to do it.
 * @author chasman
 *
 */

public interface EdgeCollapser {
	
	/**
	 * Implemented edge collapsers.
	 * @author chasman
	 *
	 */
	public enum Collapser {
		IF_CONSISTENT_COLLAPSER("IfConsistentCollapser");
		
		private String name;
		private Collapser(String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
		
		/**
		 * Makes a Collapser by name.
		 * @param name
		 * @return
		 */
		public static Collapser fromName(String name) {
			for (Collapser c : Collapser.values()) {
				if (name.equals(c.getName())) {
					return c;
				}
			}
			return null;
		}
		
		/**
		 * Make an edge collapser with args (nodes for special treatment)
		 * @param args
		 * @return
		 */
		public EdgeCollapser make(String[] args) {
			switch(this) {
			case IF_CONSISTENT_COLLAPSER: return new IfConsistentCollapser(args);
			}
			return null;
		}
		
		/**
		 * Make an edge collapser without args
		 * @return
		 */
		public EdgeCollapser make() {
			switch(this) {
			case IF_CONSISTENT_COLLAPSER: return new IfConsistentCollapser();
			}
			return null;
		}
	}
	
	
	/**
	 * Attempts to collapse an edge library. 
	 * Returns a collapsed version.
	 * Each implementation of the EdgeCollapser does this differently.
	 * @param original	original edges
	 * @return	map from original to new library edges
	 */
	public HashMap<Edge, HashSet<Edge>> collapse(EdgeLibrary original);

	/**
	 * Make all undirected edges involving this (these) node(s) 
	 * into directed edges going forward.
	 * @param node
	 */
	public void doForwardDirection(String node);
	public void doForwardDirection(Collection<String> nodes);
	
	/**
	 * Query the EdgeCollapser about whether it's doing redirection.
	 * @return
	 */
	public boolean doRedirecting();
	public HashMap<Edge, HashSet<Edge>> redirect(EdgeLibrary original);
	
}
