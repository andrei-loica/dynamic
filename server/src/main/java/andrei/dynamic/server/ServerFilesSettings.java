package andrei.dynamic.server;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Andrei
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class ServerFilesSettings {
    
    @XmlElement(name = "group")
    private FileGroup[] groups;

    public FileGroup[] getGroups() {
	return groups;
    }

    public void setGroups(FileGroup[] groups) {
	this.groups = groups;
    }
    
}
