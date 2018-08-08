package andrei.dynamic.client;

import andrei.dynamic.common.MessageType;
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

    public MessageFromServer(byte type, byte[] content) {

	this.type = MessageType.parseCode(type);
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
