package structures;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

import utilities.Enums.Sign;
import utilities.GenUtils;
import exceptions.DuplicateException;
import exceptions.InvalidValueException;

/**
 * Stores (double) scores for each ordered pair. 
 * Can be read in from a file 
 * or constructed out of two ordered PairDirectories - scoring the overlap between
 * the seconds of each pair of firsts.
 * @author dchasman
 *
 */
public class ScoredPairDirectory extends PairDirectory {
	
	protected final HashMap<String, HashMap<String, Double>> scores;

	protected ScoredPairDirectory(String filename, String name) {
		super(filename, name);
		this.scores=new HashMap<String, HashMap<String, Double>>();
	}
	
	/**
	 * Adds a scored, signed pair.
	 * Overwrites existing.
	 * @param a
	 * @param b
	 * @param s
	 * @return pre-existing score, if overwritten
	 */
	protected double add(String a, String b, Sign s, double score) {
		Sign overwrite = super.add(a, b, s);		
		if (!this.scores.containsKey(a)) {
			this.scores.put(a, new HashMap<String, Double>());
		} 		
		Double prev = this.scores.get(a).put(b, score);
		return prev; 
	}
	
	/**
	 * Adds a scored, unsigned pair to the directory. Overwrites existing. 
	 * @param a
	 * @param b
	 * @param score
	 * @return
	 */
	protected double add(String a, String b, double score) {
		return this.add(a, b, Sign.UNKNOWN, score); 
	}
	
	public double getScore(String a, String b) {
		if (this.contains(a, b)) {
			return this.scores.get(a).get(b);
		} 
		else {
			throw new NullPointerException(String.format("No scored pair %s->%s", a,b));
		}
	}
	
	
	/**
	 * Reads ordered, possibly signed, scored pairs.
	 * Format:
	 * a	b	[sign]	score
	 * The strings you use to define positive/negative relationships should
	 * be provided as posText and negText. All others will be interpreted as
	 * "unknown".
	 * 
	 * If you are reading source-target pairs in which targets are provided as 
	 * regular gene ORFs or IDs, you may wish to provide a format string 
	 * to change their names to distinguish them from the protein version.
	 * E.g., for gene targets in source-target pairs, I may provide the
	 * string "%s_D" or "G_%".
	 * 	
	 * @param filename
	 * @param targetFormat	to add special format to targets; e.g., "%s_D" for DNA
	 * @param readSigns	true if we want to check the third column for sign of relationship
	 * @param posText text for positive sign
	 * @param negText text for negative sign
	 * @return
	 */
	public static ScoredPairDirectory readScoredPairs(String filename, String name, String targetFormat, boolean readSigns,
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

		ScoredPairDirectory pairs = new ScoredPairDirectory(filename, name);
		while (s.hasNext()) {
			String[] sp = s.nextLine().trim().split("\t");
			if (sp.length==0) {
				continue;
			}
			if (sp[0].startsWith("#")) {
				continue;
			}
			if (sp.length < 3) {
				throw new InvalidValueException(
						String.format("Reading scored pairs from %s: " +
								"Improper line '%s'\n", 
								filename, Arrays.toString(sp)));
			}

			// format target
			String nodeA = sp[0], nodeB = String.format(targetFormat, sp[1]);				
			Sign sign = Sign.UNKNOWN;
			int scoreCol=2;
			double score = 0.0;
			if (readSigns && sp.length > 2 && signMap.containsKey(sp[2]) ) {
				sign = signMap.get(sp[2]);
				if (pairs.hasSign(nodeA, nodeB) && sign != pairs.getSign(nodeA, nodeB)) {
					throw new DuplicateException(
							String.format(
									"We already specified a different sign for %s->%s (%s, now %s)",
									nodeA, nodeB, sign, pairs.getSign(nodeA, nodeB)));
				}		
				scoreCol=3;
			}	
			
			try {
				score = Double.parseDouble(sp[scoreCol]);
			} catch (NumberFormatException nfe) {
				throw new InvalidValueException(String.format("Bad score format: %f", sp[scoreCol]));
			}
			
			double over = pairs.add(nodeA, nodeB, sign, score);	
		}

		return pairs;	
	}
	
	/**
	 * Creates a ScoredPairDirectory out of two existing PairDirectories.
	 * Assumption is that the two directories have shared targets.
	 * Pairs in new directory are firsts from the two original PairDirectories.
	 * @param one
	 * @param two
	 * @return
	 */
	public static ScoredPairDirectory scoreJaccard(PairDirectory one, PairDirectory two, String name) {
		String comboFile = String.format("%s.%s", one.filename, two.filename);
		ScoredPairDirectory scored=new ScoredPairDirectory(comboFile, name);
		
		for (String oneFirst : one.getFirsts()) {
			Set<String> oneSeconds = one.getSeconds(oneFirst);
			for (String twoFirst : two.getFirsts()) {
				Set<String> twoSeconds = two.getSeconds(twoFirst);
				double jaccard = GenUtils.jaccard(oneSeconds, twoSeconds);
				scored.add(oneFirst, twoFirst, jaccard);
			}
		}
		return scored;
	}


}
