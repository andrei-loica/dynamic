package andrei.dynamic.common;

/**
 *
 * @author Andrei
 */
public interface DirectoryChangesListener {
    
    public void onFileDeleted(final AbstractContentNode file);
    
    public void onFileCreated(final AbstractContentNode file);
    
    public void onFileModified(final AbstractContentNode file);
    
}
