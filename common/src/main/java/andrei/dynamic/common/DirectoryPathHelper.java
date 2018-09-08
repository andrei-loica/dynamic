package andrei.dynamic.common;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 *
 * @author Andrei
 */
public class DirectoryPathHelper {

    private final String tempDir;
    private final String absoluteRoot;

    public DirectoryPathHelper(final String absoluteRoot, final String tempDir) {
	if (absoluteRoot.charAt(absoluteRoot.length() - 1) == '\\'
		|| absoluteRoot.charAt(absoluteRoot.length() - 1) == '/') {
	    this.absoluteRoot = absoluteRoot.substring(0, absoluteRoot.length()
		    - 1);
	} else {
	    this.absoluteRoot = absoluteRoot;
	}
	if (tempDir == null || tempDir.isEmpty()) {
	    this.tempDir = absoluteRoot + "/temp";
	} else if (tempDir.charAt(tempDir.length() - 1) == '\\' || tempDir.
		charAt(tempDir.length() - 1) == '/') {
	    this.tempDir = tempDir.substring(0, tempDir.length() - 1);
	} else {
	    this.tempDir = tempDir;
	}
    }

    public String root() {
	return absoluteRoot;
    }

    public String relativeFilePath(final String absolute) throws Exception {
	if (!absolute.startsWith(absoluteRoot)) {
	    throw new PathMatchException();
	}

	return absolute.substring(absoluteRoot.length(), absolute.length()).
		replace('\\', '/');
    }

    public String getAbsolutePath(final String relative) throws Exception {
	return absoluteRoot + normalizeRelativePath(relative);
    }

    public static String normalizeRelativePath(final String relative) throws
	    Exception {
	String result = relative.replace('\\', '/');
	if (result.equals("/")) {
	    return result;
	}
	if (result.charAt(result.length() - 1) == '/') {
	    result = result.substring(0, result.length() - 1);
	}

	if (result.charAt(0) != '/') {
	    return '/' + result;
	}

	return result;
    }

    public Path getTempFilePath(final String relativePath) throws Exception {
	Path absolute = FileSystems.getDefault().getPath(tempDir,
		relativePath.trim());
	Files.createDirectories(absolute.getParent());
	try {
	    (new File(absolute.toString())).createNewFile();
	} catch (FileAlreadyExistsException ex) {
	    //nimic
	}

	return absolute;
    }

    @SuppressWarnings("empty-statement")
    public static byte[] getLocalFileMD5(final String absolute) throws Exception {

	final MessageDigest digest = MessageDigest.getInstance("MD5");

	try (FileInputStream fileInput
		= new FileInputStream(new File(absolute));
		DigestInputStream stream = new DigestInputStream(fileInput,
			digest)) {
	    final byte[] dummyBuff = new byte[16384]; //1024 * 16
	    while (stream.read(dummyBuff, 0, dummyBuff.length) != -1);
	} catch (Exception ex) {
	    return null;
	}

	return digest.digest();
    }

}
