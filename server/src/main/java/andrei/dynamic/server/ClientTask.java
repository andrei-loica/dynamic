package andrei.dynamic.server;

/**
 *
 * @author Andrei
 */
public class ClientTask {
    
    public final ClientTaskType type;
    public final Object object;
    
    public ClientTask(final ClientTaskType type, final Object object){
	this.type = type;
	this.object = object;
    }
    
}
