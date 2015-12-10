package structures;

import java.util.HashMap;

/**
 * We represent nodes just using strings, but this class
 * allows us to keep track of only one instance of each unique string.
 * 
 * @author chasman
 *
 */
public class Node {
	private static final HashMap<String, String> nodes=new HashMap<String,String>();
	private Node() {
	}
	
	public static String makeNode(String node) {
		node=node.toUpperCase().trim(); // uggggggh
		if (!Node.nodes.containsKey(node)) 
			nodes.put(node, node);
		return Node.nodes.get(node);
	}

}
