package andrei.dynamic.common;

/**
 * First 4 bits of a message.
 * 
 * @author Andrei
 */
public enum MessageType {
    
    /**
     * CONTENT:
     * <ul>
     *	<li>operation: 4 bits</li>
     *	<li>file name length: 1 byte</li>
     *	<li>file name: up to 255 bytes</li>
     *	<li><i>content length: 2 bytes (only for WRITE operation)</i></li>
     *	<li><i>up to 65535 bytes of content (only for WRITE operation)</i></li>
     * </ul>
     */
    TRANSFER(0),
    
    /**
     * CONTENT:
     * <ul>
     *	<li>read buffer size: 4 bytes</li>
     *	<li>write
     * </ul>
     */
    SHAKE(1),
    SHAKE_ACK(5),
    SHUTDOWN(2),
    SHUTDOWN_ACK(6),
    REMAINING(3),
    ERROR(7),
    
    ;
    
    private final int code;
    
    private MessageType(int code){
	this.code = code;
    }
    
    public int getCode(){
	return code;
    }
    
    public MessageType parseCode(int code){
	for (MessageType type : MessageType.values()){
	    if (type.code == code){
		return type;
	    }
	}
	
	return null;
    }
    
}
