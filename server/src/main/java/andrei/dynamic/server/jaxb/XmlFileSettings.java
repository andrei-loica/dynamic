package andrei.dynamic.server.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author Andrei
 */
@XmlAccessorType(XmlAccessType.NONE)
public class XmlFileSettings {
    
    @XmlAttribute
    private String rootDirectory;
    
    @XmlAttribute
    private int maxDirectoryDepth;
    
    @XmlAttribute
    private int checkPeriodMillis;
    
    @XmlElement(name = "file-group")
    private XmlFileGroup[] groups;

    
    public String getRootDirectory() {
	return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
	this.rootDirectory = rootDirectory;
    }

    public int getMaxDirectoryDepth() {
	return maxDirectoryDepth;
    }

    public void setMaxDirectoryDepth(int maxDirectoryDepth) {
	this.maxDirectoryDepth = maxDirectoryDepth;
    }

    public int getCheckPeriodMillis() {
	return checkPeriodMillis;
    }

    public void setCheckPeriodMillis(int checkPeriodMillis) {
	this.checkPeriodMillis = checkPeriodMillis;
    }
    
    public XmlFileGroup[] getGroups() {
	return groups;
    }

    public void setGroups(XmlFileGroup[] groups) {
	this.groups = groups;
    }
    
}
