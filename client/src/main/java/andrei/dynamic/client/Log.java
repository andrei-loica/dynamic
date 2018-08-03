package andrei.dynamic.client;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author Andrei
 */
public class Log {
    
    private static Logger log = null;
    
    public static void setup(final String logFile, final Level level) throws Exception {
	if (level == Level.FINE || level == Level.INFO || level == Level.WARNING){
	    log.setLevel(level);
	} else {
	    throw new Exception("unknown logging level");
	}
	
	log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	for (Handler handler : log.getHandlers()){
	    log.removeHandler(handler);
	}
	
	final FileHandler handler = new FileHandler(logFile);
	handler.setFormatter(new SimpleFormatter());
	//TODO
    }
    
    public static boolean isFineEnabled(){
	return log.isLoggable(Level.FINE);
    }
    
    public static boolean isInfoEnabled(){
	return log.isLoggable(Level.INFO);
    }
    
    public static void warn(final String msg){
	if (log != null){
	    log.warning(msg);
	}
    }
    
    public static void info(final String msg){
	if (log != null)
	log.info(msg);
    }
    
    public static void fine(final String msg){
	if (log != null)
	log.fine(msg);
    }
}
