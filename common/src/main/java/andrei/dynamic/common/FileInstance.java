package andrei.dynamic.common;

import java.io.File;

/**
 *
 * @author Andrei
 */
public class FileInstance extends AbstractContentNode{

    public FileInstance(final File file) throws Exception {
	super(file.getAbsolutePath(), file.lastModified());
	
	if (!file.isFile()){
	    throw new Exception(getPath() + " is not a file");
	}
    }

    @Override
    public boolean isDirectory() {
	return false;
    }
    
}
