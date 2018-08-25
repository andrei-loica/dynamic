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
    private String remoteControlAddress;
    
    @XmlElement
    private int remoteControlPort;
    
    @XmlElement
    private String remoteDataAddress;
    
    @XmlElement
    private int remoteDataPort;
    
    @XmlElement
    private boolean keepAlive;
    
    @XmlElement
    private String directoryPath;
    
    @XmlElement
    private String clientAuthToken;
    
    @XmlElement
    private String key;
    
    @XmlElement
    private String logLevel;
    
    @XmlElement
    private String logLocation;

    
    public String getRemoteControlAddress() {
	return remoteControlAddress;
    }

    public void setRemoteControlAddress(String remoteControlAddress) {
	this.remoteControlAddress = remoteControlAddress;
    }

    public int getRemoteControlPort() {
	return remoteControlPort;
    }

    public void setRemoteControlPort(int remoteControlPort) {
	this.remoteControlPort = remoteControlPort;
    }

    public String getRemoteDataAddress() {
	return remoteDataAddress;
    }

    public void setRemoteDataAddress(String remoteDataAddress) {
	this.remoteDataAddress = remoteDataAddress;
    }

    public int getRemoteDataPort() {
	return remoteDataPort;
    }

    public void setRemoteDataPort(int remoteDataPort) {
	this.remoteDataPort = remoteDataPort;
    }

    public boolean getKeepAlive() {
	return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
	this.keepAlive = keepAlive;
    }

    public String getDirectoryPath() {
	return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
	this.directoryPath = directoryPath;
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
    
}
