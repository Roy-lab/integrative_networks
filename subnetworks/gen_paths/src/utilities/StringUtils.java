package utilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;


/**
 * Useful functions for makin' strings. like joining a collection.
 * @author chasman
 *
 */

public class StringUtils {

	/**
	 * Joins string representations of items with a delimiter.
	 * @param <T>
	 * @param col
	 * @param delim
	 * @return
	 */
	public static <T> String join(Collection<T> col, String delim) {
		StringBuilder sb=new StringBuilder();
		Iterator<T> iter = col.iterator();
		if (iter.hasNext()) {
			sb.append(iter.next().toString());
			while (iter.hasNext()) {
				sb.append(String.format("%s%s", delim, iter.next().toString()));
			}
		}		
		return sb.toString();
	}
	
	/**
	 * Joins after sorting by STRING order.
	 * @param <T>
	 * @param col
	 * @param delim
	 * @return
	 */
	public static <T> String sortJoin(Collection<T> col, String delim) {
		ArrayList<String> colList = new ArrayList<String>();
		for (T item : col) {
			colList.add(item.toString());
		}		
		Collections.sort(colList);
		return join(colList, delim);
	}
	
		
}
