package andrei.dynamic.common;

import java.io.File;

/**
 *
 * @author Andrei
 */
public class FileInstance
	extends AbstractContentNode {

    public FileInstance(final File file, final DirectoryInstance parent) throws
	    Exception {
	super(file.getAbsolutePath(), file.lastModified(), parent);

	if (!file.isFile()) {
	    throw new Exception(getPath() + " is not a file");
	}
    }

    @Override
    public boolean isDirectory() {
	return false;
    }

}
