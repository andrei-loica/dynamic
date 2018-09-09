package andrei.dynamic.common;

import java.util.Objects;

/**
 *
 * @author Andrei
 */
public abstract class AbstractContentNode
	implements Comparable<AbstractContentNode> {

    private final String path;
    private final long modifiedDate;
    private final DirectoryInstance parent;

    public AbstractContentNode(final String path, long lastModifiedDate,
	    final DirectoryInstance parent) {
	if (path.charAt(path.length() - 1) == '\\' || path.charAt(path.length()
		- 1) == '/') {
	    this.path = path.substring(0, path.length() - 1);
	} else {
	    this.path = path;
	}
	this.parent = parent;
	modifiedDate = lastModifiedDate;
    }

    public abstract boolean isDirectory();

    public String getPath() {
	return path;
    }

    public final long getLastModifiedDate() {
	return modifiedDate;
    }
    
    public final DirectoryInstance getParent(){
	return parent;
    }

    @Override
    public int compareTo(AbstractContentNode other) {
	int comp = getPath().compareTo(other.getPath());
	if (comp != 0) {
	    return comp;
	}
	if (isDirectory()) {
	    if (other.isDirectory()) {
		return 0;
	    }
	    return -1;
	} else if (other.isDirectory()) {
	    return 1;
	}
	return 0;

    }

    @Override
    public int hashCode() {
	int hash = 7;
	hash = 83 * hash + Objects.hashCode(this.getPath());
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
	final AbstractContentNode other = (AbstractContentNode) obj;
	return (Objects.equals(this.getPath(), other.getPath()) && this.isDirectory()
		== other.isDirectory());
    }

}
