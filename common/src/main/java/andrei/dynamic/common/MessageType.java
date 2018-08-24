package andrei.dynamic.common;

/**
 * First 4 bits of a message.
 * 
 * @author Andrei
 */
public enum MessageType {
    
    TEST_MESSAGE(100),
    DELETE_FILE_MESSAGE(102),
    UPDATE_FILE_MESSAGE(103),
    CHECK_FILE_MESSAGE(104),
    TEST_MESSAGE_RSP(110),
    CHECK_FILE_MESSAGE_RSP(114),
    TRANSFER_PADDING(120),
    SET_PERMISSION_MESSAGE(150),
    CLOSING_MESSAGE(200)
    
    ;
    
    private final int code;
    
    private MessageType(int code){
	this.code = code;
    }
    
    public int getCode(){
	return code;
    }
    
    public static MessageType parseCode(int code){
	for (MessageType type : MessageType.values()){
	    if (type.code == code){
		return type;
	    }
	}
	
	return null;
    }
    
}
