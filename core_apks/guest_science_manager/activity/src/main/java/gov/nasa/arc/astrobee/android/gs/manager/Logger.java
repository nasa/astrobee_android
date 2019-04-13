package gov.nasa.arc.astrobee.android.gs.manager;

/**
 * Created by kmbrowne on 11/17/17.
 */

public class Logger {
    org.apache.commons.logging.Log mLog;

    public Logger() {}

    public Logger(org.apache.commons.logging.Log log) {
        mLog = log;
    }

    public void setRosLogger(org.apache.commons.logging.Log log) {
        mLog = log;
    }

    // Debug output
    public void debug(String tag, String msg) {
        android.util.Log.d(tag, msg);
        mLog.debug(msg);
    }

    public void debug(String tag, String msg, Throwable tr) {
        android.util.Log.d(tag, msg, tr);
        mLog.debug(msg, tr);
    }

    // Error output
    public void error(String tag, String msg) {
        android.util.Log.e(tag, msg);
        mLog.error(msg);
    }

    public void error(String tag, String msg, Throwable tr) {
        android.util.Log.e(tag, msg, tr);
        mLog.error(msg, tr);
    }

    // Fatal/what a terrible failure output
    public void fatal(String tag, String msg) {
        android.util.Log.wtf(tag, msg);
        mLog.fatal(msg);
    }

    public void fatal(String tag, Throwable tr) {
        android.util.Log.wtf(tag, tr);
        mLog.fatal(tag, tr);
    }

    public void fatal(String tag, String msg, Throwable tr) {
        android.util.Log.wtf(tag, msg, tr);
        mLog.fatal(msg, tr);
    }

    // Info output
    public void info(String tag, String msg) {
        android.util.Log.i(tag, msg);
        mLog.info(msg);
    }

    public void info(String tag, String msg, Throwable tr) {
        android.util.Log.i(tag, msg, tr);
        mLog.info(msg, tr);
    }

    // Verbose output
    public void verbose(String tag, String msg) {
        android.util.Log.v(tag, msg);
        mLog.debug(msg);
    }

    public void verbose(String tag, String msg, Throwable tr) {
        android.util.Log.v(tag, msg);
        mLog.debug(msg, tr);
    }

    // Warn output
    public void warn(String tag, String msg) {
        android.util.Log.w(tag, msg);
        mLog.warn(msg);
    }

    public void warn(String tag, Throwable tr) {
        android.util.Log.w(tag, tr);
        mLog.warn(tag, tr);
    }

    public void warn(String tag, String msg, Throwable tr) {
        android.util.Log.w(tag, msg, tr);
        mLog.warn(msg, tr);
    }
}
