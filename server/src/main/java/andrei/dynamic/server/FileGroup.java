package andrei.dynamic.server;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author Andrei
 */
public class FileGroup {
    
    @XmlAttribute
    private String name;
    
    @XmlElement(name = "file")
    private FileGroupElement[] files;

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public FileGroupElement[] getFiles() {
	return files;
    }

    public void setFiles(FileGroupElement[] files) {
	this.files = files;
    }
    
    
    
}
