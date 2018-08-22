package andrei.dynamic.server.jaxb;

import andrei.dynamic.server.jaxb.XmlFileSettings;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Andrei
 */
@XmlRootElement(name = "server-configuration")
@XmlAccessorType(XmlAccessType.NONE)
public class XmlServerConfiguration {
    
    @XmlElement
    private int localControlPort;
    
    @XmlElement
    private int localDataPort;
    
    @XmlElement
    private int localHttpPort;
    
    @XmlElement
    private int maxClientConnections;
    
    @XmlElement
    private String key;
    
    @XmlElement(name = "file-settings")
    private XmlFileSettings fileSettings;
    

    public int getLocalControlPort() {
	return localControlPort;
    }

    public void setLocalControlPort(int localControlPort) {
	this.localControlPort = localControlPort;
    }

    public int getLocalDataPort() {
	return localDataPort;
    }

    public void setLocalDataPort(int localDataPort) {
	this.localDataPort = localDataPort;
    }

    public int getLocalHttpPort() {
	return localHttpPort;
    }

    public void setLocalHttpPort(int localHttpPort) {
	this.localHttpPort = localHttpPort;
    }

    public int getMaxClientConnections() {
	return maxClientConnections;
    }

    public void setMaxClientConnections(int maxClientConnections) {
	this.maxClientConnections = maxClientConnections;
    }

    public String getKey() {
	return key;
    }

    public void setKey(String key) {
	this.key = key;
    }

    public XmlFileSettings getFileSettings() {
	return fileSettings;
    }

    public void setFileSettings(XmlFileSettings fileSettings) {
	this.fileSettings = fileSettings;
    }
    
}
