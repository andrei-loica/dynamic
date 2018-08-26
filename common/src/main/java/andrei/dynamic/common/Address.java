package andrei.dynamic.common;

import java.util.Objects;

/**
 *
 * @author Andrei
 */
public class Address
	implements Comparable<Address> {

    private final String host;
    private final int port;

    public Address(final String host, int portValue) {
	if (host == null) {
	    throw new NullPointerException("null address value");
	}

	port = portValue;
	if (host.endsWith(":" + port)) {
	    if (host.charAt(0) == '/' || host.charAt(0) == '\\') {
		this.host = host.substring(1, host.length() - (":" + port).
			length());
	    } else {
		this.host = host.substring(0, host.length() - (":" + port).
			length());
	    }
	} else {
	    if (host.charAt(0) == '/' || host.charAt(0) == '\\') {
		this.host = host.substring(1);
	    } else {
		this.host = host;
	    }
	}
    }

    public String getHost() {
	return host;
    }

    public int getPort() {
	return port;
    }

    @Override
    public String toString() {
	return host + ":" + port;
    }

    @Override
    public int hashCode() {
	int hash = 7;
	hash = 83 * hash + Objects.hashCode(this.host);
	hash = 83 * hash + this.port;
	return hash;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final Address other = (Address) obj;
	if (this.port != other.port) {
	    return false;
	}
	if (!Objects.equals(this.host, other.host)) {
	    return false;
	}
	return true;
    }

    @Override
    public int compareTo(Address other) {
	return toString().compareTo(other.toString());
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
