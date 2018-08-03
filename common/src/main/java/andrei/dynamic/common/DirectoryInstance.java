package andrei.dynamic.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Andrei
 */
public class DirectoryInstance
	extends AbstractContentNode {

    private final AbstractContentNode[] content;

    public DirectoryInstance(final File file, int maxDepth) throws Exception { //le si sorteaza
	super(file.getAbsolutePath(), file.lastModified());

	if (!file.isDirectory()) {
	    throw new Exception(getPath() + " is not a directory");
	}

	if (maxDepth == 0) {
	    content = null;
	    return;
	}

	File[] actualContent = file.listFiles();
	
	content = new AbstractContentNode[actualContent.length];

	for (int i = 0; i < actualContent.length; i++) {
	    if (actualContent[i].isDirectory()) {
		content[i] = new DirectoryInstance(actualContent[i], maxDepth
			- 1);
	    } else if (actualContent[i].isFile()) {
		content[i] = new FileInstance(actualContent[i]);
	    }
	}
	Arrays.sort(content);
    }

    @Override
    public boolean isDirectory() {
	return true;
    }

    public AbstractContentNode[] getContent() {
	return content;
    }

    public ArrayList<AbstractContentNode> getAllFiles(int maxDepth) {
	final ArrayList<AbstractContentNode> files = new ArrayList<>();

	for (int i = 0; i < content.length; i++) {
	    if (content[i].isDirectory() && maxDepth > 0){
		files.addAll(((DirectoryInstance) content[i]).getAllFiles(maxDepth - 1));
	    } else {
		files.add(content[i]);
	    }
	}

	return files;
    }

}
