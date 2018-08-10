package andrei.dynamic.server.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 * @author Andrei
 */
@XmlAccessorType(XmlAccessType.NONE)
public class FileGroupElement {
    
    @XmlAttribute
    private String localPath;
    
    @XmlAttribute
    private String remotePath;

    public String getLocalPath() {
	return localPath;
    }

    public void setLocalPath(String localPath) {
	this.localPath = localPath;
    }

    public String getRemotePath() {
	return remotePath;
    }

    public void setRemotePath(String remotePath) {
	this.remotePath = remotePath;
    }
    
    
    
}
