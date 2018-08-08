package andrei.dynamic.server;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author Andrei
 */
@XmlAccessorType(XmlAccessType.NONE)
public class FileGroup {
    
    @XmlAttribute(name = "client")
    private String client;
    
    @XmlElement(name = "file")
    private FileGroupElement[] files;

    public String getClient() {
	return client;
    }

    public void setClient(String client) {
	this.client = client;
    }

    public FileGroupElement[] getFiles() {
	return files;
    }

    public void setFiles(FileGroupElement[] files) {
	this.files = files;
    }
    
    
    
}
