package andrei.dynamic.common;

import java.util.Arrays;

/**
 *
 * @author Andrei
 */
public class MessageFactory {

    public static final int STD_MSG_DIM = 256;
    public static final int TRANSFER_PADDING_MDG_DIM = 32;
    public static final int CHECK_FILE_MSG_RSP_DIM = 512;

    private MessageFactory() {

    }
    
    public static int dimForType(int type){
	switch (type){
	    case 104:
	    case 102:
	    case 100:
	    case 110:
	    case 103:
		return STD_MSG_DIM;
	    case 114:
		return CHECK_FILE_MSG_RSP_DIM;
	    case 120:
		return TRANSFER_PADDING_MDG_DIM;
	    default:
		return STD_MSG_DIM;
		
	}
    }
    
    public static int dimForType(MessageType type){
	switch (type){
	    case CHECK_FILE_MESSAGE:
	    case DELETE_FILE_MESSAGE:
	    case TEST_MESSAGE:
	    case TEST_MESSAGE_RSP:
	    case UPDATE_FILE_MESSAGE:
		return STD_MSG_DIM;
	    case CHECK_FILE_MESSAGE_RSP:
		return CHECK_FILE_MSG_RSP_DIM;
	    case TRANSFER_PADDING:
		return TRANSFER_PADDING_MDG_DIM;
	    default:
		return STD_MSG_DIM; //NOK
	}
    }

    public static byte[] newDeletedFileMessage(final String relativeName) throws
	    Exception {
	byte[] content;
	try {
	    content = relativeName.getBytes("US-ASCII");
	} catch (Exception ex) {
	    Log.warn("failed to get string as ASCII for " + relativeName, ex);
	    return null;
	}

	if (content.length >= STD_MSG_DIM) {
	    throw new Exception("too big file relative name " + relativeName
		    + " for deleted file message");
	}

	return resolvePadding(concatenate(
		(byte) MessageType.DELETE_FILE_MESSAGE.
			getCode(), content), STD_MSG_DIM);
    }

    public static byte[] newUpdatedFileMessage(final String relativeName)
	    throws Exception {
	byte[] content;
	try {
	    content = relativeName.getBytes("US-ASCII");
	} catch (Exception ex) {
	    Log.warn("failed to get string as ASCII for " + relativeName, ex);
	    return null;
	}

	if (content.length >= STD_MSG_DIM) {
	    throw new Exception("too big file relative name " + relativeName
		    + " for modify file message");
	}

	return resolvePadding(concatenate(
		(byte) MessageType.UPDATE_FILE_MESSAGE.
			getCode(), content), STD_MSG_DIM);
    }

    public static byte[] newTransferPaddingMessage(final int padding)
	    throws Exception {
	if (padding > 15) {
	    throw new IllegalArgumentException("padding value too big");
	}
	byte[] content = new byte[2];
	content[0] = (byte) MessageType.TRANSFER_PADDING.getCode();
	content[1] = (byte) padding;

	return resolvePadding(content, TRANSFER_PADDING_MDG_DIM);
    }

    public static byte[] newCheckFileMessage(final String relativeName)
	    throws Exception {
	byte[] content;
	content = relativeName.getBytes("US-ASCII"); //TODO: verifica inainte daca e ASCII

	if (content.length >= STD_MSG_DIM) {
	    throw new Exception("too big file relative name " + relativeName
		    + " for modify file message");
	}

	return resolvePadding(concatenate(
		(byte) MessageType.CHECK_FILE_MESSAGE.
			getCode(), content), STD_MSG_DIM);
    }

    public static byte[] newCheckFileMessageResponse(final String relativeName,
	    final byte[] md5)
	    throws Exception {
	byte[] content;
	try {
	    content = relativeName.getBytes("US-ASCII");
	} catch (Exception ex) {
	    Log.warn("failed to get string as ASCII for " + relativeName, ex);
	    return null;
	}

	if (content.length >= STD_MSG_DIM) {
	    throw new Exception("too big file relative name " + relativeName
		    + " for modify file message");
	}

	if (md5 != null && md5.length == 16) {
	    return resolvePadding(concatenate(resolvePadding(concatenate(
		    (byte) MessageType.CHECK_FILE_MESSAGE_RSP.getCode(), content),
		    STD_MSG_DIM), md5), CHECK_FILE_MSG_RSP_DIM);
	}

	return resolvePadding(new byte[]{
	    (byte) MessageType.CHECK_FILE_MESSAGE_RSP.getCode()},
		CHECK_FILE_MSG_RSP_DIM);
    }

    public static byte[] newTestMessage() {
	byte[] content = new byte[1];

	content[0] = (byte) MessageType.TEST_MESSAGE.getCode();
	return resolvePadding(content, STD_MSG_DIM);
    }

    public static byte[] newTestMessageResponse(final String authToken) throws
	    Exception {
	byte[] content;
	try {
	    content = authToken.getBytes("US-ASCII");
	} catch (Exception ex) {
	    Log.warn("failed to get string as ASCII for " + authToken, ex);
	    return null;
	}

	if (content.length >= STD_MSG_DIM) {
	    throw new Exception("too big file relative name " + authToken
		    + " for modify file message");
	}

	return resolvePadding(concatenate(
		(byte) MessageType.TEST_MESSAGE_RSP.
			getCode(), content), STD_MSG_DIM);
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

    /*@SuppressWarnings("empty-statement")
    public static byte[] trimPadding(final byte[] bytes, int last,
	    int maxPadding) {

	if (last - maxPadding > 0) {
	    maxPadding = last - maxPadding;
	} else {
	    maxPadding = 0;
	}
	int i = last;
	while (i > maxPadding && bytes[--i] == 0);

	return Arrays.copyOfRange(bytes, 0, i);

    }*/

    public static byte[] concatenate(final byte[]... arrays) {
	if (arrays == null || arrays.length == 0) {
	    return null;
	}
	if (arrays.length == 1) {
	    return arrays[0];
	}

	int length = 0;
	for (byte[] arr : arrays) {
	    length += arr.length;
	}
	byte[] result = new byte[length];

	int current = 0;
	for (byte[] arr : arrays) {
	    System.arraycopy(arr, 0, result, current, arr.length);
	    current += arr.length;
	}

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
