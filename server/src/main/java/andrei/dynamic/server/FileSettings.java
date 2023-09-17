package andrei.dynamic.server;

import andrei.dynamic.server.jaxb.XmlFileGroup;
import andrei.dynamic.server.jaxb.XmlFileSettings;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Andrei
 */
public class FileSettings {
    private String rootDirectory;
    private int maxDirectoryDepth;
    private int checkPeriodMillis;
    private ArrayList<XmlFileGroup> groups;
    
    public FileSettings(){
	
    }
    
    public FileSettings(final XmlFileSettings original){
	rootDirectory = original.getRootDirectory();
	maxDirectoryDepth = original.getMaxDirectoryDepth();
	checkPeriodMillis = original.getCheckPeriodMillis();
        if (original.getGroups() != null){
	groups = new ArrayList(Arrays.asList(original.getGroups()));
        } else {
            groups = new ArrayList();
        }
    }

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

    public ArrayList<XmlFileGroup> getGroups() {
	return groups;
    }

    public void setGroups(ArrayList<XmlFileGroup> groups) {
	this.groups = groups;
    }
}
