package andrei.dynamic.server;

/**
 *
 * @author Andrei
 */
public class MustResetConnectionException
	extends Exception {

    /**
     * Creates a new instance of <code>MustResetConnectionException</code>
     * without detail message.
     */
    public MustResetConnectionException() {
    }

    /**
     * Constructs an instance of <code>MustResetConnectionException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public MustResetConnectionException(String msg) {
	super(msg);
    }
}
