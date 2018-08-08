package andrei.dynamic.server;

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
public class ServerConfiguration {
    
    @XmlElement
    private int localControlPort;
    
    @XmlElement
    private int localDataPort;
    
    @XmlElement
    private int maxClientConnections;
    
    @XmlElement(name = "file-settings")
    private FileSettings fileSettings;
    

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

    public int getMaxClientConnections() {
	return maxClientConnections;
    }

    public void setMaxClientConnections(int maxClientConnections) {
	this.maxClientConnections = maxClientConnections;
    }

    public FileSettings getFileSettings() {
	return fileSettings;
    }

    public void setFileSettings(FileSettings fileSettings) {
	this.fileSettings = fileSettings;
    }
    
}
