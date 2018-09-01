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
    private String localAddress;
    
    @XmlElement
    private int localControlPort;
    
    @XmlElement
    private int localHttpPort;
    
    @XmlElement
    private int maxClientConnections;
    
    @XmlElement
    private String key;
    
    @XmlElement
    private String logLevel;
    
    @XmlElement
    private String logLocation;
    
    @XmlElement
    private boolean logAppend;
    
    @XmlElement
    private String webId;
    
    @XmlElement
    private String webPass;
    
    @XmlElement
    private int webLoginExpTime;
    
    @XmlElement(name = "file-settings")
    private XmlFileSettings fileSettings;

    
    public String getLocalAddress() {
	return localAddress;
    }

    public void setLocalAddress(String localAddress) {
	this.localAddress = localAddress;
    }

    public int getLocalControlPort() {
	return localControlPort;
    }

    public void setLocalControlPort(int localControlPort) {
	this.localControlPort = localControlPort;
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

    public XmlFileSettings getFileSettings() {
	return fileSettings;
    }

    public void setFileSettings(XmlFileSettings fileSettings) {
	this.fileSettings = fileSettings;
    }

    public String getWebId() {
	return webId;
    }

    public void setWebId(String webId) {
	this.webId = webId;
    }

    public String getWebPass() {
	return webPass;
    }

    public void setWebPass(String webPass) {
	this.webPass = webPass;
    }

    public int getWebLoginExpTime() {
	return webLoginExpTime;
    }

    public void setWebLoginExpTime(int webLoginExpTime) {
	this.webLoginExpTime = webLoginExpTime;
    }
    
}
