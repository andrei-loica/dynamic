package andrei.dynamic.common;

import java.util.Arrays;

/**
 *
 * @author Andrei
 */
public class MessageFactory {

    public static final byte INIT_MSG_CODE = 0x64;
    public static final byte CHECK_FILE_MSG_CODE = 0x6E;
    public static final int CHECK_FILE_MSG_DIM = 129;

    private MessageFactory() {

    }

    public static byte[] createInitialMessage() {
	byte[] content = new byte[1];
	content[0] = INIT_MSG_CODE;

	return content;
    }

    public static byte[] createCheckFileMessage(final String relativeName) {
	byte[] content;
	try {
	    content = relativeName.getBytes("US-ASCII"); //TODO: verifica inainte daca e ASCII
	} catch (Exception ex) {
	    //TODO: e naspa
	    System.err.println("failed to get as ASCII");
	    return null;
	}

	return addPadding(concatenate(INIT_MSG_CODE, content));
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

    public static byte[] addPadding(final byte[] message) {
	if (message.length >= CHECK_FILE_MSG_DIM) {
	    if (message.length > CHECK_FILE_MSG_DIM) {
		System.err.println("too big message: " + message.length
			+ " bytes");
	    }
	    return message;
	}

	byte[] padding = new byte[CHECK_FILE_MSG_DIM - message.length];
	Arrays.fill(padding, 0, CHECK_FILE_MSG_DIM - message.length, (byte) 0);

	return concatenate(message, padding);
    }

}
