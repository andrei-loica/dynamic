package andrei.dynamic.common;

/**
 *
 * @author Andrei
 */
public abstract class AbstractContentNode implements Comparable<AbstractContentNode>{

    private final String path;
    private final long modifiedDate;

    public AbstractContentNode(final String path, long lastModifiedDate) {
	this.path = path;
	modifiedDate = lastModifiedDate;
    }

    public abstract boolean isDirectory();

    public final String getPath() {
	return path;
    }

    public final long getLastModifiedDate() {
	return modifiedDate;
    }
    
    @Override
    public int compareTo(AbstractContentNode other){ //differs from equals
	return path.compareTo(other.path);
    }
}
