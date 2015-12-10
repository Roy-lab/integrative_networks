package exceptions;

/**
 * Made so I have a name for invalid values in my files.
 * @author chasman
 *
 */
public class InvalidValueException extends Exception {
	
	public InvalidValueException(String message) {
		super(message);
	}
}

