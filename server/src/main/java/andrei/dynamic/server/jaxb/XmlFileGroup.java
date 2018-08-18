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
public class XmlFileGroup {
    
    public static int index;
    
    @XmlElement(name = "client")
    private String[] clients;
    
    @XmlElement(name = "file")
    private XmlFileGroupElement[] files;
    
    private int order;

    public String[] getClients() {
	return clients;
    }

    public void setClients(String[] clients) {
	this.clients = clients;
    }

    public XmlFileGroupElement[] getFiles() {
	return files;
    }

    public void setFiles(XmlFileGroupElement[] files) {
	this.files = files;
    }

    public int getOrder() {
	return order;
    }

    public void setOrder(int order) {
	this.order = order;
    }
    
}
