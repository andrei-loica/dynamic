package andrei.dynamic.client;

import andrei.dynamic.common.MessageType;
import andrei.dynamic.common.MustResetConnectionException;
import java.util.Arrays;

/**
 *
 * @author Andrei
 */
public class MessageFromServer {

    private MessageType type;
    private byte[] content;

    public MessageFromServer() {

    }

    public MessageFromServer(int type, byte[] content) throws Exception {

	if ((this.type = MessageType.parseCode(type)) == null) {
	    throw new Exception("unknown message type " + type);
	}
	this.content = content;
    }

    public MessageType getType() {
	return type;
    }

    public void setType(MessageType type) {
	this.type = type;
    }

    public byte[] getContent() {
	return content;
    }

    public void setContent(byte[] content) {
	this.content = content;
    }

}
