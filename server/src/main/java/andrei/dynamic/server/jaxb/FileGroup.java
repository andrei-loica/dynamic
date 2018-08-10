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
public class FileGroup {
    
    @XmlElement(name = "client")
    private String[] clients;
    
    @XmlElement(name = "file")
    private FileGroupElement[] files;

    public String[] getClients() {
	return clients;
    }

    public void setClient(String[] clients) {
	this.clients = clients;
    }

    public FileGroupElement[] getFiles() {
	return files;
    }

    public void setFiles(FileGroupElement[] files) {
	this.files = files;
    }
    
    
    
}
