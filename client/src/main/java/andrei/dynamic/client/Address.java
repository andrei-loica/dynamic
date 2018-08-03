package andrei.dynamic.client;

/**
 *
 * @author Andrei
 */
public class Address {
    
    private final String host;
    private final int port;
    
    public Address(final String host, int portValue) {
	if (host == null) {
	    throw new NullPointerException("null address value");
	}
	
	this.host = host;
	port = portValue;
    }
    
    public String getHost() {
	return host;
    }
    
    public int getPort() {
	return port;
    }
    
    public static Address parseAddress(final String entry) throws Exception {
	
	if (entry.matches("\\A(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\."
		+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\."
		+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\."
		+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\z") || entry.matches(
			"\\Alocalhost\\z")) {
	    
	    throw new Exception("missing port number");
	}
	
	if (!entry.matches(
		"\\A([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}):"
		+ "([0-9]{1,5})\\z") && !entry.matches(
			"\\Alocalhost:([0-9]{1,5})\\z")) {
	    
	    throw new Exception("invalid address format");
	}
	
	if (!entry.matches("\\A(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\."
		+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\."
		+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\."
		+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2}):"
		+ "([0-9]{1,5})\\z") && !entry.matches(
			"\\Alocalhost:([0-9]{1,5})\\z")) {
	    
	    throw new Exception("invalid host");
	}
	
	int colonIndex = entry.indexOf(':');
	final String host = entry.substring(0, colonIndex);
	final int portNumber;
	try {
	    portNumber = Integer.parseInt(entry.substring(colonIndex + 1));
	} catch (Exception ex) {
	    throw new Exception("invalid port value");
	}
	
	if (portNumber > 65535 || portNumber < 0) {
	    throw new Exception("invalid port value");
	}
	
	return new Address(host, portNumber);
    }
}
