package andrei.dynamic.client;

/**
 *
 * @author Andrei
 */
public class TerminatedException
	extends Exception {

    /**
     * Creates a new instance of <code>TerminatedException</code> without detail
     * message.
     */
    public TerminatedException() {
    }

    /**
     * Constructs an instance of <code>TerminatedException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public TerminatedException(String msg) {
	super(msg);
    }
}
