package utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import structures.Pair;
import exceptions.DuplicateException;
import exceptions.InvalidValueException;

/**
 * General utility methods.
 * @author chasman
 *
 */
public class GenUtils {
	

	/**
	 * Given a set of things, return a set consisting of all pairs
	 * of those things (pairs are in natural order).
	 * @param <T>
	 * @param input
	 * @return
	 */
	public static <T extends Comparable<T>> Set<Pair<T,T>> allPairs(Set<T> input) {
		Set<Pair<T,T>> pairs = new HashSet<Pair<T,T>>();
		ArrayList<T> inlist = new ArrayList<T>(input);
		Collections.sort(inlist);
		
		for (int i=0; i < inlist.size(); i++) {
			for (int j=i+1; j<inlist.size(); j++) {
				Pair<T,T> p = new Pair<T,T>(inlist.get(i), inlist.get(j));
				pairs.add(p);
			}
		}
		
		return pairs;
	}
	
	/**
	 * Reads a file of strings into a set.
	 * @param filename
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Set<String> readFileSet(String filename, String comment) throws FileNotFoundException {
		Scanner s = null;
		HashSet<String> found=new HashSet<String>();
		try {
			s = new Scanner(new File(filename));
			while (s.hasNext()) {
				String line = s.nextLine().trim();
				if (line.startsWith(comment)) continue;
				found.add(line);
			}
		} catch (FileNotFoundException fnfe) {
			throw new FileNotFoundException("Unable to read strings from requested file " + filename);
		}
		return found;
	}
	
	/**
     * Calculates Jaccard similarity between two sets of objects.
	 * @param a
	 * @param b
	 * @return
	 */
	public static <T> double jaccard(Set<T> a, Set<T> b) {
		HashSet<Object> intersect = new HashSet<Object>(a);
		intersect.retainAll(b);
		
		HashSet<Object> union = new HashSet<Object>(a);
		union.addAll(b);
		
		double i=1.0*intersect.size();
		double u=1.0*union.size();		
		
		return (i/u);
	}
	
	/**
	 * Reads a map from a two-column file.
	 * Assumes items in column 1 are unique. (throws invalidvalue if not)
	 * Allows # lines as comments.
	 * @param filename
	 * @param delimiter
	 * @return
	 */
	public static HashMap<String, String> readMap(String filename, String delim) throws IOException, InvalidValueException, DuplicateException {
		HashMap<String,String> map=new HashMap<String,String>();
		Scanner f = new Scanner(new File(filename));	
		while (f.hasNext()) {
			String line = f.nextLine().trim();
			
			if (line.startsWith("#")) continue;
			
			String[] sp=line.split(delim);
			if (sp.length < 2) {
				throw new InvalidValueException("Map files must have at least two columns " +
						"(any more will be ignored");
			}
			if (map.containsKey(sp[1])) {
				throw new DuplicateException("Map files must be unique in column 0.");
			}
			map.put(sp[0], sp[1]);
		}
		return map;
	}

}
