package andrei.dynamic.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Andrei
 */
@XmlRootElement(name = "client-configuration")
@XmlAccessorType(XmlAccessType.NONE)
public class ClientConfiguration {
    @XmlElement
    private String localAddress;
    
    @XmlElement
    private int localPort;
    
    @XmlElement
    private String serverAddress;
    
    @XmlElement
    private int serverControlPort;
    
    @XmlElement
    private int serverDataPort;
    
    @XmlElement
    private boolean keepAlive;
    
    @XmlElement
    private String rootDirectory;
    
    @XmlElement
    private String clientAuthToken;
    
    @XmlElement
    private String key;
    
    @XmlElement
    private String logLevel;
    
    @XmlElement
    private String logLocation;
    
    @XmlElement
    private boolean logAppend;

    
    public String getLocalAddress() {
	return localAddress;
    }

    public void setLocalAddress(String localAddress) {
	this.localAddress = localAddress;
    }

    public int getLocalPort() {
	return localPort;
    }

    public void setLocalPort(int localPort) {
	this.localPort = localPort;
    }
    
    public String getServerAddress() {
	return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
	this.serverAddress = serverAddress;
    }

    public int getServerControlPort() {
	return serverControlPort;
    }

    public void setServerControlPort(int serverControlPort) {
	this.serverControlPort = serverControlPort;
    }

    public int getServerDataPort() {
	return serverDataPort;
    }

    public void setServerDataPort(int serverDataPort) {
	this.serverDataPort = serverDataPort;
    }

    public boolean getKeepAlive() {
	return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
	this.keepAlive = keepAlive;
    }

    public String getRootDirectory() {
	return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
	this.rootDirectory = rootDirectory;
    }

    public String getClientAuthToken() {
	return clientAuthToken;
    }

    public void setClientAuthToken(String clientAuthToken) {
	this.clientAuthToken = clientAuthToken;
    }

    public String getKey() {
	return key;
    }

    public void setKey(String key) {
	this.key = key;
    }

    public String getLogLevel() {
	return logLevel;
    }

    public void setLogLevel(String logLevel) {
	this.logLevel = logLevel;
    }

    public String getLogLocation() {
	return logLocation;
    }

    public void setLogLocation(String logLocation) {
	this.logLocation = logLocation;
    }

    public boolean isLogAppend() {
	return logAppend;
    }

    public void setLogAppend(boolean logAppend) {
	this.logAppend = logAppend;
    }
    
}
