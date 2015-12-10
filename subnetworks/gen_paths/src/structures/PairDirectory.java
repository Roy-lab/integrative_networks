package structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import utilities.Enums.Sign;
import exceptions.DuplicateException;
import exceptions.InvalidValueException;

/**
 * Holds information about indirect relationships between pairs of nodes.
 * Information is definitely directed, and maybe signed.
 * @author chasman
 *
 */
public class PairDirectory {

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

	protected final String filename, name;
	
	protected final HashMap<String, HashMap<String, Sign>> orderedPairs;

	protected PairDirectory(String filename, String name) {
		this.filename=filename;
		this.name=name;
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
	 * Returns the set of first items of pairs. 
	 * @return
	 */
	public Set<String> getFirsts() {
		return Collections.unmodifiableSet(this.orderedPairs.keySet());
	}
	
	/**
	 * Returns all second items for a given first item.
	 * @return
	 */
	public Set<String> getSeconds(String first){
		return Collections.unmodifiableSet(
				this.orderedPairs.get(first).keySet());
	}
	
	/**
	 * Returns the set of ALL seconds in all pairs.
	 * @return
	 */
	public Set<String> getSeconds() {
		HashSet<String> seconds = new HashSet<String>();
		for (HashMap<String, Sign> secondSet : this.orderedPairs.values()) {
			seconds.addAll(secondSet.keySet());
		}
		return seconds;
	}

	/**
	 * Given two nodes, return the order stored here.
	 * If A->B present but not B->A, return "above".
	 * If B->A present without A->B, return "below".
	 * Otherwise, return "unrestricted". 
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public PartialOrder getOrder(String a, String b) {		
		boolean fw = (this.orderedPairs.containsKey(a) && this.orderedPairs.get(a).containsKey(b));
		boolean back = (this.orderedPairs.containsKey(b) && this.orderedPairs.get(b).containsKey(a));

		if (fw && !back) return PartialOrder.ABOVE;
		else if (back && !fw) return PartialOrder.BELOW;
		else return PartialOrder.UNRESTRICTED;
	}
	
	/**
	 * Returns true if pair in directory.
	 * @param a	first item
	 * @param b	second item
	 * @return	true if pair (a,b) in directory
	 */
	public boolean contains(String a, String b) {
		if (this.orderedPairs.containsKey(a)) {
			return this.getSeconds(a).contains(b);
		}
		return false;
	}
	
	/**
	 * Tests whether or not we've defined an interaction between
	 * a node and itself.
	 * 
	 * @param a
	 * @return	true if (a,a) was in our input set of pairs
	 */
	public boolean hasSelfPair(String a) {
		return (this.orderedPairs.containsKey(a) && this.orderedPairs.get(a).containsKey(a));
	}

	/**
	 * Returns the sign stored for a->b.
	 * @param a
	 * @param b
	 * @return
	 */
	public Sign getSign(String a, String b) {
		if (this.hasSign(a, b)) {
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
	public boolean hasSign(String a, String b) {
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
	 * Reads ordered, possibly signed pairs.
	 * Format:
	 * a	b	[sign]
	 * The strings you use to define positive/negative relationships should
	 * be provided as posText and negText. All others will be interpreted as
	 * "unknown".
	 * 
	 * If you are reading source-target pairs in which targets are provided as 
	 * regular gene ORFs or IDs, you may wish to provide a format string 
	 * to change their names to distinguish them from the protein version.
	 * E.g., for gene targets in source-target pairs, I will provide the
	 * string "%s_D".
	 * 	
	 * @param filename
	 * @param targetFormat	to add special format to targets; e.g., "%s_D" for DNA
	 * @param readSigns	true if we want to check the third column for sign of relationship
	 * @param posText text for positive sign
	 * @param negText text for negative sign
	 * @return
	 */
	public static PairDirectory readOrderedPairs(String filename, String name, String targetFormat, boolean readSigns,
			String posText, String negText)
	throws InvalidValueException, DuplicateException, FileNotFoundException {

		// if we're reading signs, then supply our text
		HashMap<String,Sign> signMap = null;
		if (readSigns) {
			signMap = new HashMap<String,Sign>();
			if (posText != null) signMap.put(posText, Sign.POSITIVE);
			if (negText != null) signMap.put(negText, Sign.NEGATIVE);
		}


		Scanner s = null;
		try {
			s = new Scanner(new File(filename));
		} catch (FileNotFoundException fnfe) {
			throw new FileNotFoundException("Unable to read ordered pair file " + filename);
		}

		PairDirectory pairs = new PairDirectory(filename, name);
		while (s.hasNext()) {
			String[] sp = s.nextLine().trim().split("\t");
			if (sp.length==0) {
				continue;
			}
			if (sp[0].startsWith("#")) {
				continue;
			}
			if (sp.length < 2) {
				throw new InvalidValueException(
						String.format("Reading pairs from %s: " +
								"Improper line '%s'\n", 
								filename, Arrays.toString(sp)));
			}

			// format target
			String nodeA = sp[0], nodeB = String.format(targetFormat, sp[1]);		
			nodeA = Node.makeNode(nodeA);
			nodeB = Node.makeNode(nodeB);
			
			Sign sign = Sign.UNKNOWN;
			if (readSigns && sp.length > 2 && signMap.containsKey(sp[2]) ) {
				sign = signMap.get(sp[2]);
				if (pairs.hasSign(nodeA, nodeB) && sign != pairs.getSign(nodeA, nodeB)) {
					throw new DuplicateException(
							String.format(
									"We already specified a different sign for %s->%s (%s, now %s)",
									nodeA, nodeB, sign, pairs.getSign(nodeA, nodeB)));
				}
			}				
			Sign over = pairs.add(nodeA, nodeB, sign);	
		}

		return pairs;	
	}

	/**
	 * Read ordered pairs without sign info or target formatting.
	 * @param filename
	 */
	public static PairDirectory readOrderedPairs(String filename, String name)
	throws InvalidValueException, DuplicateException, FileNotFoundException {
		return PairDirectory.readOrderedPairs(filename, name, "%s", false, null, null);
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
	public static PairDirectory readIndirectory(String filename, String name) 
	throws InvalidValueException, DuplicateException, FileNotFoundException {
		Scanner s = null;
		try {
			s = new Scanner(new File(filename));
		} catch (FileNotFoundException fnfe) {
			throw new FileNotFoundException("Unable to read Indirectory file " + filename);
		}

		PairDirectory indirectory = new PairDirectory(filename, name);
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

			if (indirectory.hasSign(nodeA, nodeB)) {
				throw new DuplicateException(
						String.format(
								"We already specified a sign for %s->%s (%s, now %s)",
								nodeA, nodeB, event, indirectory.getSign(nodeA, nodeB)));
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

	/**
	 * Prints a summary of the PairDirectory.
	 */
	public String toString() {
		String str = String.format(
				"PairDirectory %s.\n\tContains %d pairs. %d sources, %d unique targets.",
				filename, this.size(), this.getFirsts().size(), this.getSeconds().size());
		return str;		
	}
	
	/**
	 * Prints the directory out to a tab-delim format.
	 */
	public String tabFormat() {
		StringBuilder sb = new StringBuilder(String.format("#PairDirectory %s:\n", filename));
		for (String f : this.getFirsts()) {
			for (String s : this.getSeconds(f)) {
				sb.append(String.format("%s\t%s\t%s\n", f, s, getSign(f,s)));
			}
		}		
		return sb.toString();
	}


}
