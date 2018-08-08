package andrei.dynamic.common;

/**
 *
 * @author Andrei
 */
public class PathMatchException
	extends Exception {

    /**
     * Creates a new instance of <code>PathMatchException</code> without detail
     * message.
     */
    public PathMatchException() {
    }

    /**
     * Constructs an instance of <code>PathMatchException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public PathMatchException(String msg) {
	super(msg);
    }
}
