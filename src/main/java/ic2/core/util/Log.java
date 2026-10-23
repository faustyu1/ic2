package ic2.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {
    public static final Log INSTANCE = new Log();
    private final Logger logger = LogManager.getLogger("IC2");

    public void warn(LogCategory cat, String msg, Object... args) {
        try { logger.warn("["+cat+"] "+String.format(msg.replace("%s","%s"), args)); } catch(Exception e) { logger.warn("["+cat+"] "+msg); }
    }
    public void warn(LogCategory cat, Throwable t, String msg, Object... args) {
        try { logger.warn("["+cat+"] "+String.format(msg, args), t); } catch(Exception e) { logger.warn("["+cat+"] "+msg, t); }
    }
    public void info(LogCategory cat, String msg, Object... args) {
        try { logger.info("["+cat+"] "+String.format(msg, args)); } catch(Exception e) { logger.info("["+cat+"] "+msg); }
    }
    public void debug(LogCategory cat, String msg, Object... args) {
        try { logger.debug("["+cat+"] "+String.format(msg, args)); } catch(Exception e) { logger.debug("["+cat+"] "+msg); }
    }
    public void debug(LogCategory cat, Throwable t, String msg) {
        logger.debug("["+cat+"] "+msg, t);
    }
    public void debug(LogCategory cat, Throwable t, String msg, Object... args) {
        try { logger.debug("["+cat+"] "+String.format(msg, args), t); } catch(Exception e) { logger.debug("["+cat+"] "+msg, t); }
    }
    public java.io.PrintStream getPrintStream(LogCategory cat, org.apache.logging.log4j.Level level) {
        return System.out;
    }
}
