package exceptions;

/**
 * To be thrown if we try to use an unordered feature/value in an 
 * ordered way.
 * @author chasman
 *
 */
public class IncomparableException extends Exception {
	public IncomparableException(String message) {
		super(message);
	}
}
