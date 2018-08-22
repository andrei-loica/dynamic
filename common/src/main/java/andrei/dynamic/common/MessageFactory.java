package andrei.dynamic.common;

import java.util.Arrays;

/**
 *
 * @author Andrei
 */
public class MessageFactory {

    public static final int UPDATE_FILE_MSG_DIM = 256;
    public static final int TEST_MSG_DIM = 256;

    private MessageFactory() {

    }

    public static byte[] newDeletedFileMessage(final String relativeName) throws
	    Exception {
	byte[] content;
	try {
	    content = relativeName.getBytes("US-ASCII"); //TODO: verifica inainte daca e ASCII
	} catch (Exception ex) {
	    //TODO: e naspa
	    System.err.println("failed to get name as ASCII");
	    return null;
	}

	if (content.length >= UPDATE_FILE_MSG_DIM) {
	    throw new Exception("too big file relative name " + relativeName
		    + " for deleted file message");
	}

	return resolvePadding(concatenate(
		(byte) MessageType.DELETE_FILE_MESSAGE.
			getCode(), content), UPDATE_FILE_MSG_DIM);
    }

    public static byte[] newCreatedFileMessage(final String relativeName) throws
	    Exception {
	byte[] content;
	try {
	    content = relativeName.getBytes("US-ASCII"); //TODO: verifica inainte daca e ASCII
	} catch (Exception ex) {
	    //TODO: e naspa
	    System.err.println("failed to get name as ASCII");
	    return null;
	}

	if (content.length >= UPDATE_FILE_MSG_DIM) {
	    throw new Exception("too big file relative name " + relativeName
		    + " for created file message");
	}

	return resolvePadding(concatenate(
		(byte) MessageType.CREATE_FILE_MESSAGE.
			getCode(), content), UPDATE_FILE_MSG_DIM);
    }

    public static byte[] newModifiedFileMessage(final String relativeName)
	    throws Exception {
	byte[] content;
	try {
	    content = relativeName.getBytes("US-ASCII"); //TODO: verifica inainte daca e ASCII
	} catch (Exception ex) {
	    //TODO: e naspa
	    System.err.println("failed to get name as ASCII");
	    return null;
	}

	if (content.length >= UPDATE_FILE_MSG_DIM) {
	    throw new Exception("too big file relative name " + relativeName
		    + " for modify file message");
	}

	return resolvePadding(concatenate(
		(byte) MessageType.MODIFY_FILE_MESSAGE.
			getCode(), content), UPDATE_FILE_MSG_DIM);
    }

    public static byte[] newTestMessage() {
	byte[] content = new byte[1];

	content[0] = (byte) MessageType.TEST_MESSAGE.getCode();
	return resolvePadding(content, TEST_MSG_DIM);
    }

    public static byte[] newTestResponseMessage(final String authToken) throws
	    Exception {
	byte[] content;
	try {
	    content = authToken.getBytes("US-ASCII"); //TODO: verifica inainte daca e ASCII
	} catch (Exception ex) {
	    //TODO: e naspa
	    System.err.println("failed to get token as ASCII");
	    return null;
	}

	if (content.length >= UPDATE_FILE_MSG_DIM) {
	    throw new Exception("too big file relative name " + authToken
		    + " for modify file message");
	}

	return resolvePadding(concatenate(
		(byte) MessageType.TEST_MESSAGE_RSP.
			getCode(), content), TEST_MSG_DIM);
    }

    public static byte[] newClosingMessage() {
	byte[] content = new byte[1];
	content[0] = (byte) MessageType.CLOSING_MESSAGE.getCode();

	return content;
    }

    @SuppressWarnings("empty-statement")
    public static byte[] trimPadding(final byte[] bytes) {
	if (bytes.length < 1 || bytes[bytes.length - 1] != 0) {
	    return bytes;
	}

	int i = bytes.length - 1;
	while (i > 0 && bytes[--i] == 0);

	return Arrays.copyOfRange(bytes, 0, i + 1);

    }

    public static byte[] concatenate(final byte[] arr1, final byte[] arr2) {
	byte[] result = new byte[arr1.length + arr2.length];

	System.arraycopy(arr1, 0, result, 0, arr1.length);
	System.arraycopy(arr2, 0, result, arr1.length, arr2.length);

	return result;
    }

    public static byte[] concatenate(final byte value, final byte[] arr2) {
	byte[] result = new byte[arr2.length + 1];

	result[0] = value;
	System.arraycopy(arr2, 0, result, 1, arr2.length);

	return result;
    }

    public static byte[] resolvePadding(final byte[] message, int dim) {
	if (message.length >= dim) {
	    return message;
	}

	byte[] padding = new byte[dim - message.length];
	Arrays.fill(padding, 0, dim - message.length, (byte) 0);

	return concatenate(message, padding);
    }

}
