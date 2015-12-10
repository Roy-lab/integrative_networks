package utilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Useful static methods for making GAMS code.
 * 
 */
public class GamsUtils {

	/**
	 * Strips out non-alphanumeric characters to make a GAMS-friendly identifier
	 * @param name
	 * @return
	 */
	public static String gamsCleanse(String name) {
		String newName = name.replaceAll("[^A-Za-z0-9]", "");
		return newName;
	}

	/**
	 * Prints a set list.
	 * Collapses IDs when possible.
	 * @param setName
	 * @param setDescr
	 * @param strs
	 * @param cols
	 * @return
	 */
	public static String makeSetList(String setName, String setDescr, 
			Collection<String> strs, int cols) {
		// if empty, print a placeholder.
		if (strs.size() == 0) {
			return makeEmpty(setName, setDescr);
		}

		StringBuilder sb = new StringBuilder();
		setDescr=setDescr.replace("\"","");
		sb.append(String.format("Set %s\t\"%s (%d)\"\n\t/ ", 
				setName, setDescr, strs.size()));
		boolean lineStart=true;

		// collapse strings
		strs = makeCollapsedIDSet(strs);

		ArrayList<String> strings = new ArrayList<String>(strs);		
		// sort list alphabetically
		Collections.sort(strings);

		for (int i=0; i < strings.size(); i++) {
			String n = strings.get(i);
			sb.append(String.format("%s %s", (lineStart ? "" : ","), n));
			if ((i+1) % cols == 0 && i < strings.size()-1) {
				sb.append(",\n\t");
				lineStart=true;
			} else {
				lineStart=false;
			}
		}
		sb.append(" /; \n\n");
		return sb.toString();
	}

	/**
	 * Make an empty set: set contains the null item,
	 * tuple-ified to match the setName.
	 * @param setName
	 * @param setDescr
	 */
	protected static String makeEmpty(String setName, String setDescr) {
		int size = setName.split(",").length;


		StringBuilder nulls = new StringBuilder("null");
		while (size > 1) {
			nulls.append(".null");
			size--;
		}

		setDescr=setDescr.replace("\"","");
		String empty = String.format("Set %s\t\"%s\"\t/ %s /;\n", setName, setDescr, nulls.toString());
		return empty;
	}


	protected static String makeTupleSet(String setName, String setDescr, HashMap<String, HashSet<String>> map, int cols) {
		ArrayList<String> tuples = new ArrayList<String>();
		for (String key : map.keySet()) {
			if (map.get(key).size()==0) continue;
			String tup = pathTuple(key, map.get(key), 20);
			tuples.add(tup);
		}

		return makeSetList(setName, setDescr, tuples, cols);		
	}

	/**
	 * Given:  a, [b,c,d,e...]
	 * makes the string
	 * a.(b,c,d,e,...)
	 * Inserts linebreaks after col items because GAMS is needy like that.
	 * Removes duplicate elements and sorts in alphabetical order.
	 * @param first
	 * @param others
	 * @return
	 */
	protected static String pathTuple(String first, Collection<String> others, int col) {
		// make sure is set
		HashSet<String> oset = new HashSet<String>(makeCollapsedIDSet(others));
		ArrayList<String> olist = new ArrayList<String>(oset);
		Collections.sort(olist);

		String list = olist.get(0);
		for (int i = 1; i < olist.size(); i++) {
			if (i % col == 0) {
				list =  list + ", \n" + olist.get(i); 
			} else {
				list = list + ", " + olist.get(i);
			}
		}
		String tup = String.format("%s.(%s)", first, list);
		return tup;
	}	

	/**
	 * Given a set of IDs, some of which end in numbers,
	 * collapse when possible into id0*idN format.
	 * @param ids
	 * @return
	 */
	public static HashSet<String> makeCollapsedIDSet(Collection<String> ids) {		

		// gets numbers at the end of the name
		Pattern p = Pattern.compile("(\\d+)$");

		// for each prefix, record all the values seen
		HashMap<String, ArrayList<Integer>> seen = new HashMap<String, ArrayList<Integer>>();

		// some IDs are unique and we just carry them on.
		HashSet<String> noPrefix=new HashSet<String>();


		for (String id : ids) {


			// split into text and numbers
			Matcher m = p.matcher(id);

			// if we don't find the pattern OR the item contains a period, don't do anything
			if (!m.find() || id.contains(".")) {
				noPrefix.add(id);
				continue;
			} 	
			String num = m.group(1);
			// if this starts with 0 and doesn't end with 0, don't bother consolidating.
			if (num.startsWith("0") && num.length() > 1) {
				noPrefix.add(id);
				continue;
			}

			int line = Integer.parseInt(num);
			String dataset = id.substring(0, m.start(1));

			if (!seen.keySet().contains(dataset)) {
				seen.put(dataset, new ArrayList<Integer>());
			}
			seen.get(dataset).add(line);
		}

		HashSet<String> prefices = new HashSet<String>();
		prefices.addAll(seen.keySet());		

		HashSet<String> all = new HashSet<String>();
		for (String prefix : prefices) {
			ArrayList<String> items = getCollapsedList(prefix, seen.get(prefix));
			all.addAll(items);
		}
		all.addAll(noPrefix);

		return all;
	}

	/**
	 * Prints out a parameter.
	 * @param name
	 * @param descr
	 * @param values
	 * @return
	 */
	protected static String makeParameter(String name, String descr, Map<String, Double> values) {
		StringBuilder sb = new StringBuilder();
		// remove " from descr if present
		descr=descr.replace("\"","");
		sb.append(String.format("Parameter %s\t\"%s (%d)\" ", 
				name, descr, values.size()));

		// no score? end it there
		if (values.size() == 0) {
			sb.append("; \n");
		} else {
			sb.append("/");
			for (Entry<String,Double> val : values.entrySet()) {
				sb.append(String.format("\n\t%s\t%s", val.getKey(), val.getValue()));
			}
			sb.append("\n/;\n");
		}
		return sb.toString();

	}

	/**
	 * Given:
	 * prefix, [2, 17, 18, 19, 42, 43, 44]
	 * Return:
	 * [prefix2, prefix17*prefix19, prefix42*prefix44]
	 * @param prefix
	 * @param numbers
	 */
	protected static ArrayList<String> getCollapsedList(String prefix, ArrayList<Integer> numbers) {
		ArrayList<String> collapsed = new ArrayList<String>();

		// if only one number, this is easy
		// if < 3 numbers, don't bother with the star.
		if (numbers.size()<=2) {
			for (Integer num : numbers) {
				String newItem = String.format("%s%d",  prefix, num);
				collapsed.add(newItem);
			}
			return collapsed;
		}


		Collections.sort(numbers);
		int start=numbers.get(0); // value beginning this range
		int si = 0;	//index of beginning value
		int i = 1;
		for (i=1; i < numbers.size(); i++) {
			// if we skip more than one value, make one or more new items from value "start" to the previous one.
			// make two if we only moved two.
			if (numbers.get(i) > numbers.get(i-1)+1) {
				//String newItem = "";	

				// if the beginning of the range is more than one item ago
				if (si < i-2) { //start != numbers.get(i-1) && start != numbers.get(i-2)) {
					collapsed.add(String.format("%s%d*%s%d", prefix, start, prefix, numbers.get(i-1)));
				} else {
					// must have been one or two ago
					for (int j = si; j < i; j++) {
						collapsed.add(String.format("%s%d", prefix, numbers.get(j)));
					}
				}
				//collapsed.add(newItem);
				start=numbers.get(i);
				si=i;
			}			
		}
		// make the last one. could have been one number, two, or more.
		// one or two? print separately
		if (si >= numbers.size()-3) {	
			for (int j =si; j < numbers.size(); j++) {
				String newItem = String.format("%s%d", prefix, numbers.get(j));
				collapsed.add(newItem);
			}
		} 
		// more than two? starify
		else {
			String newItem = String.format("%s%d*%s%d", prefix, start, prefix, numbers.get(numbers.size()-1));
			collapsed.add(newItem);
		}			

		// testing
		//System.out.println(numbers);
		//System.out.println(collapsed);

		return collapsed;		
	}
}
