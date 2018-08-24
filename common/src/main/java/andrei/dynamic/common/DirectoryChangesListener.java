package andrei.dynamic.common;

/**
 *
 * @author Andrei
 */
public interface DirectoryChangesListener {
    
    public void onFileLoaded(final FileInstance file);
    
    public void onFileCreated(final FileInstance file);
    
    public void onFileModified(final FileInstance file);
    
    public void onFileDeleted(final FileInstance file);
    
}
