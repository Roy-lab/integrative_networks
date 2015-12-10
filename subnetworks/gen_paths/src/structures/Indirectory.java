package structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

import utilities.Enums.Sign;
import exceptions.DuplicateException;
import exceptions.InvalidValueException;

/**
 * Holds information about indirect relationships between pairs of nodes.
 * Information is definitely directed, and maybe signed.
 * @author chasman
 *
 */
public class Indirectory {
	
	/**
	 * Defines a partial order for a relationship between two nodes.
	 * @author chasman
	 *
	 */
	public enum PartialOrder {
		ABOVE, // Node A > Node B
		BELOW, // Node A < Node B
		UNRESTRICTED;	// No restrictions on order
	}
	
	protected final String filename;
	protected final HashMap<String, HashMap<String, Sign>> orderedPairs;
	
	protected Indirectory(String filename) {
		this.filename=filename;
		this.orderedPairs=new HashMap<String, HashMap<String, Sign>>();
	}
	/**
	 * Adds a relationship to the Indirectory.
	 * Overwrites existing.
	 * @param a
	 * @param b
	 * @param s
	 * @return pre-existing sign, if overwritten
	 */
	protected Sign add(String a, String b, Sign s) {
		if (!this.orderedPairs.containsKey(a)) {
			this.orderedPairs.put(a, new HashMap<String, Sign>());
		} 		
		Sign overwrite = this.orderedPairs.get(a).put(b, s);
		return overwrite; 
	}
	
	/**
	 * Given two nodes, return the order stored here.
	 * If A->B present but not B->A, return "above".
	 * If B->A present without A->B, return "below".
	 * Otherwise, return "unrestricted".
	 * @param a
	 * @param b
	 * @return
	 */
	public PartialOrder requiredOrder(String a, String b) {
		boolean fw = (this.orderedPairs.containsKey(a) && this.orderedPairs.get(a).containsKey(b));
		boolean back = (this.orderedPairs.containsKey(b) && this.orderedPairs.get(b).containsKey(a));
		
		if (fw && !back) return PartialOrder.ABOVE;
		else if (back && !fw) return PartialOrder.BELOW;
		else return PartialOrder.UNRESTRICTED;
	}
	
	/**
	 * Returns the sign stored for a->b.
	 * @param a
	 * @param b
	 * @return
	 */
	public Sign requiredSign(String a, String b) {
		if (this.specifiesSign(a, b)) {
			return this.orderedPairs.get(a).get(b);
		} else {
			return Sign.UNKNOWN;
		}
	}
	
	/**
	 * Returns true if there is a specified sign for the interaction a->b.
	 * @param a
	 * @param b
	 * @return
	 */
	public boolean specifiesSign(String a, String b) {
		return this.orderedPairs.containsKey(a) && this.orderedPairs.get(a).containsKey(b);
	}
	
	/**
	 * Returns the number of pairs in the indirectory.
	 * @return
	 */
	public int size() {
		int size=0;
		for (HashMap<String, Sign> inner : orderedPairs.values()) {
			size += inner.size();
		}
		return size;
	}
	
	public String filename() {
		return this.filename;
	}
	
		
	/**
	 * Reads a set of indirect, directed relationships from a file.
	 * Expected input format (starting from column startCol) 
	 * EventType	NodeA	NodeB	
	 * 
	 * Event types: "Positive_regulation", "Regulation", "Negative_regulation" 
	 * 
	 * @param filename
	 * @return
	 * @throws InvalidValueException
	 */
	public static Indirectory readIndirectory(String filename, int startCol) 
	throws InvalidValueException, DuplicateException, FileNotFoundException {
		Scanner s = null;
		try {
			s = new Scanner(new File(filename));
		} catch (FileNotFoundException fnfe) {
			throw new FileNotFoundException("Unable to read Indirectory file " + filename);
		}
		
		Indirectory indirectory = new Indirectory(filename);
		while (s.hasNext()) {
			String[] sp = s.nextLine().trim().split("\t");
			if (sp.length==0) {
				continue;
			}
			if (sp[0].startsWith("#")) {
				continue;
			}
		
			String event = sp[0];
			String nodeA = sp[1], nodeB = sp[2];
			
			if (indirectory.specifiesSign(nodeA, nodeB)) {
				throw new DuplicateException(
						String.format(
								"We already specified a sign for %s->%s (%s, now %s)",
								nodeA, nodeB, event, indirectory.requiredSign(nodeA, nodeB)));
			}
			Sign read=null;
			if (event.equals("Regulation")) {
				read=Sign.UNKNOWN;
			} else if (event.equals("Positive_regulation")) {
				read=Sign.POSITIVE;
			} else if (event.equals("Negative_regulation")) {
				read=Sign.NEGATIVE;
			} else {
				throw new InvalidValueException("Unknown event type: " + event);
			}
			Sign over = indirectory.add(nodeA, nodeB, read);	
			assert(over==null) : 
				"A duplicate slipped in?";
		}
		
		return indirectory;
	}
	

}
