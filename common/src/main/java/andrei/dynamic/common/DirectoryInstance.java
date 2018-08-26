package andrei.dynamic.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Andrei
 */
public class DirectoryInstance
	extends AbstractContentNode {

    private final AbstractContentNode[] content;

    public DirectoryInstance(final File file, int maxDepth,
	    final DirectoryInstance parent) throws Exception { //le si sorteaza
	super(file.getAbsolutePath(), file.lastModified(), parent);

	if (!file.isDirectory()) {
	    throw new Exception(getPath() + " is not a directory");
	}

	if (maxDepth == 0) {
	    content = null;
	    return;
	}

	File[] actualContent = file.listFiles();

	final ArrayList<AbstractContentNode> arr = new ArrayList();

	for (int i = 0; i < actualContent.length; i++) {
	    if (actualContent[i].isDirectory() && maxDepth > 1) {
		arr.add(new DirectoryInstance(actualContent[i], maxDepth
			- 1, this));
	    } else if (actualContent[i].isFile()) {
		arr.add(new FileInstance(actualContent[i], this));
	    }
	}
	content = new AbstractContentNode[arr.size()];
	arr.toArray(content);
	
	Arrays.sort(content);
    }

    @Override
    public boolean isDirectory() {
	return true;
    }

    public AbstractContentNode[] getContent() {
	return content;
    }

    public ArrayList<FileInstance> getAllFiles(int maxDepth) {
	final ArrayList<FileInstance> files = new ArrayList<>();

	for (int i = 0; i < content.length; i++) {
	    if (content[i].isDirectory()) {
		if (maxDepth > 1) {
		    files.addAll(((DirectoryInstance) content[i]).getAllFiles(
			    maxDepth - 1));
		}
	    } else {
		files.add((FileInstance) content[i]);
	    }
	}

	return files;
    }

}
