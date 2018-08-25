package andrei.dynamic.common;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author Andrei
 */
public class Log {

    private static final Formatter FORMATTER;

    private static Level level;
    private static Handler handler;

    static {
	FORMATTER = new SimpleFormatter() {

	    @Override
	    public synchronized String format(LogRecord lr) {
		return String.format("[%1$tF %1$tT] [%2$s] %3$s %n", new Date(
			lr.getMillis()), lr.getLevel().getLocalizedName(), lr.
			getMessage()
		);
	    }

	};

	level = null;
	handler = null;
    }

    private Log() {
    }

    public static void setLevel(final String level) {
	switch (level.toUpperCase()) {
	    case "OFF":
		Log.level = CustomLevel.OFF;
		break;
	    case "TRACE":
		Log.level = CustomLevel.TRACE;
		break;
	    case "DEBUG":
		Log.level = CustomLevel.DEBUG;
		break;
	    case "FINE":
		Log.level = CustomLevel.FINE;
		break;
	    case "INFO":
		Log.level = CustomLevel.INFO;
		break;
	    case "WARNING":
		Log.level = CustomLevel.WARNING;
		break;
	    case "FATAL":
		Log.level = CustomLevel.FATAL;
		break;
	    default:
		throw new IllegalArgumentException("unknown logging level");
	}
	if (handler != null) {
	    handler.setLevel(Log.level);
	}
    }

    public static void setLevel(final Level level) {
	Log.level = level;
	if (handler != null) {
	    handler.setLevel(Log.level);
	}
    }

    public static Level getLevel() {
	return level;
    }

    public static void setFile(final String logFile) throws Exception {
	handler = new FileHandler(logFile, false);
	handler.setFormatter(FORMATTER);
	if (level != null) {
	    handler.setLevel(level);
	}
    }

    public static void setStdOutput() {
	handler = new ConsoleHandler();
	handler.setFormatter(FORMATTER);
	if (level != null){
	    handler.setLevel(level);
	}
    }
    
    public static void close(){
	handler.flush();
	try {
	handler.close();
	} catch (Exception ex){
	    //nimic
	}
    }
    
    public static boolean isTraceEnabled(){
	return level == CustomLevel.TRACE;
    }
    
    public static boolean isDebugEnabled(){
	return (level == CustomLevel.DEBUG || level == CustomLevel.TRACE);
    }
    
    public static void log(Level logLevel, final String log){
	final LogRecord record = new LogRecord(logLevel, log);
	handler.publish(record);
    }

    public static void trace(final String log) {
	log(CustomLevel.TRACE, log);
    }

    public static void debug(final String log) {
	log(CustomLevel.DEBUG, log);
    }

    public static void fine(final String log) {
	log(CustomLevel.FINE, log);
    }

    public static void info(final String log) {
	log(CustomLevel.INFO, log);
    }

    public static void warn(final String log) {
	log(CustomLevel.WARNING, log);
    }

    public static void fatal(final String log) {
	log(CustomLevel.FATAL, log);
    }

    public static void trace(final String log, final Throwable ex) {
	log(CustomLevel.TRACE, log + ": " + ex.getClass() + " " + ex.getMessage());
    }

    public static void debug(final String log, final Throwable ex) {
	log(CustomLevel.DEBUG, log + ": " + ex.getClass() + " " + ex.getMessage());
    }

    public static void fine(final String log, final Throwable ex) {
	log(CustomLevel.FINE, log + ": " + ex.getClass() + " " + ex.getMessage());
    }

    public static void info(final String log, final Throwable ex) {
	log(CustomLevel.INFO, log + ": " + ex.getClass() + " " + ex.getMessage());
    }

    public static void warn(final String log, final Throwable ex) {
	log(CustomLevel.WARNING, log + ": " + ex.getClass() + " " + ex.getMessage());
    }

    public static void fatal(final String log, final Throwable ex) {
	log(CustomLevel.FATAL, log + ": " + ex.getClass() + " " + ex.getMessage());
    }

    private static class CustomLevel
	    extends Level {

	public static final Level TRACE = new CustomLevel("TRACE", 200);
	public static final Level DEBUG = new CustomLevel("DEBUG", 300);
	public static final Level FATAL = new CustomLevel("FATAL", 1000);

	private CustomLevel(final String label, int i) {
	    super(label, i);
	}
    }

}
