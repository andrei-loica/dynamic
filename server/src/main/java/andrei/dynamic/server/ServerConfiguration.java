package andrei.dynamic.server;

import andrei.dynamic.server.jaxb.XmlFileGroup;
import andrei.dynamic.server.jaxb.XmlFileSettings;
import andrei.dynamic.server.jaxb.XmlServerConfiguration;
import java.util.Arrays;

/**
 *
 * @author Andrei
 */
public class ServerConfiguration {
    private String localAddress;
    private int localControlPort;
    private int localDataPort;
    private int localHttpPort;
    private int maxClientConnections;
    private String key;
    private FileSettings fileSettings;
    private String logLevel;
    private String logLocation;
    private boolean logAppend;
    
    public ServerConfiguration(){
	
    }
    
    public ServerConfiguration(final XmlServerConfiguration original){
	localAddress = original.getLocalAddress();
	localControlPort = original.getLocalControlPort();
	localDataPort = original.getLocalDataPort();
	localHttpPort = original.getLocalHttpPort();
	maxClientConnections = original.getMaxClientConnections();
	key = original.getKey();
	fileSettings = new FileSettings(original.getFileSettings());
	logLevel = original.getLogLevel().toUpperCase();
	logLocation = original.getLogLocation();
	logAppend = original.isLogAppend();
    }
    

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

    public FileSettings getFileSettings() {
	return fileSettings;
    }

    public void setFileSettings(FileSettings fileSettings) {
	this.fileSettings = fileSettings;
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
    
    public XmlServerConfiguration toJaxb(){
	final XmlServerConfiguration converted = new XmlServerConfiguration();
	
	converted.setLocalAddress(localAddress);
	converted.setLocalControlPort(localControlPort);
	converted.setLocalDataPort(localDataPort);
	converted.setLocalHttpPort(localHttpPort);
	converted.setMaxClientConnections(maxClientConnections);
	converted.setKey(key);
	
	final XmlFileSettings convertedSettings = new XmlFileSettings();
	convertedSettings.setCheckPeriodMillis(fileSettings.getCheckPeriodMillis());
	convertedSettings.setGroups(Arrays.copyOf(fileSettings.getGroups().toArray(), fileSettings.getGroups().size(),
		XmlFileGroup[].class));
	convertedSettings.setMaxDirectoryDepth(fileSettings.getMaxDirectoryDepth());
	convertedSettings.setRootDirectory(fileSettings.getRootDirectory());
	
	converted.setFileSettings(convertedSettings);
	converted.setLogLevel(logLevel);
	converted.setLogLocation(logLocation);
	converted.setLogAppend(logAppend);
	
	return converted;
    }
}
