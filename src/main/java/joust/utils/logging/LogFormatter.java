package joust.utils.logging;

import jlibs.core.lang.Ansi;
import jlibs.core.util.logging.AnsiFormatter;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * MyCustomFormatter formats the LogRecord as follows:
 * date   level   localized message with parameters
 */
public class LogFormatter extends AnsiFormatter {
    @Override
    public String format(LogRecord logRecord) {
        // Create a StringBuffer to contain the formatted record
        // start with the date.
        StringBuffer sb = new StringBuffer();

        Ansi ansi = ansiForLevel(logRecord.getLevel());
        sb.append(ansi.colorize('[' + logRecord.getLevel().getName() + ']')).append(' ');

        // Get the formatted message (includes localization
        // and substitution of paramters) and add it to the buffer
        sb.append(formatMessage(logRecord));
        sb.append('\n');

        return sb.toString();
    }

    private static Ansi ansiForLevel(Level level) {
        String s = level.getName();
        if ("SEVERE".equals(s)) {
            return SEVERE;
        } else if ("WARNING".equals(s)) {
            return WARNING;
        } else if ("INFO".equals(s)) {
            return INFO;
        } else if ("CONFIG".equals(s)) {
            return CONFIG;
        } else if ("FINEST".equals(s)) {
            return FINEST;
        } else if ("FINER".equals(s)) {
            return FINER;
        } else if ("FINE".equals(s)) {
            return FINE;
        }

        return SEVERE;
    }
}
