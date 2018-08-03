package andrei.dynamic.common;

/**
 *
 * @author Andrei
 */
public enum Operation {
    
    WRITE(0),
    READ(1),
    DELETE(2),
    
    ;
    
    private final int code;
    
    private Operation(int code){
	this.code = code;
    }
    
    public int getCode(){
	return code;
    }
    
    public Operation parseCode(int code){
	for (Operation type : Operation.values()){
	    if (type.code == code){
		return type;
	    }
	}
	
	return null;
    }
}
