package andrei.dynamic.server.jaxb;

import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 * @author Andrei
 */
@XmlAccessorType(XmlAccessType.NONE)
public class XmlFileGroupElement{
    
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

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 29 * hash + Objects.hashCode(this.localPath);
	hash = 29 * hash + Objects.hashCode(this.remotePath);
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
	final XmlFileGroupElement other = (XmlFileGroupElement) obj;
	if (!Objects.equals(this.localPath, other.localPath)) {
	    return false;
	}
	if (!Objects.equals(this.remotePath, other.remotePath)) {
	    return false;
	}
	return true;
    }
    
    
    
}
