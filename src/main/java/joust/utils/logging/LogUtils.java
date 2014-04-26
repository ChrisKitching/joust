package joust.utils.logging;

import joust.utils.commandline.OptimiserOptions;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static joust.utils.commandline.OptimiserOptions.logLevel;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class LogUtils {
    private static ProcessingEnvironment processingEnv;

    // The values associated with logging levels, to allow us to speedily determine if we can
    // avoid doing substitutions.
    private static final int SEVERE_VALUE = 1000;
    private static final int WARNING_VALUE = 900;
    private static final int INFO_VALUE = 800;
    private static final int CONFIG_VALUE = 700;
    private static final int FINE_VALUE = 500;
    private static final int FINER_VALUE = 400;
    private static final int FINEST_VALUE = 300;

    public static void init(ProcessingEnvironment env) {
        processingEnv = env;
        // Since the Ansi library doesn't understand zsh...
        System.setProperty("Ansi", "true");
        Logger logger = LogManager.getLogManager().getLogger("");
        logger.setLevel(OptimiserOptions.logLevel);
        Handler handler = logger.getHandlers()[0];
        handler.setLevel(OptimiserOptions.logLevel);
        handler.setFormatter(new LogFormatter());
    }

    /**
     * Raises a fatal compiler error with the given message, halting execution.
     *
     * @param error The error string to display.
     */
    private static void raiseCompilerError(String error) {
        // In the case we're aborting before the environment is available...
        if (processingEnv == null) {
            return;
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Fatal error from optimiser: " + error + "\nAt:\n" + Arrays.toString(new Exception().getStackTrace()).replace(',', '\n'));
    }

    public static class LogExtensions {
        private static String doSubstitutions(String msg, Object[] objects) {
            if (objects.length == 0) {
                return msg;
            }
            StringBuilder sb = new StringBuilder();
            int index = 0;
            for (int i = 0; i < objects.length; i++) {
                int newInd = msg.indexOf("{}", index);
                String part;
                if (newInd == -1) {
                    part = msg.substring(index);
                } else {
                    part = msg.substring(index, newInd);
                }
                sb.append(part);
                sb.append(objects[i]);
                index = newInd + 2;
            }
            sb.append(msg.substring(index));

            return sb.toString();
        }

        private static String nullSafeToString(Object o) {
            if (o == null) {
                return "null";
            }
            return o.toString();
        }

        public static void trace(Logger logger, String msg, Object... objects) {
            if (logLevel.intValue() <= FINEST_VALUE) {
                logger.finest(doSubstitutions(msg, objects));
            }
        }

        public static void trace(Logger logger, Object o) {
            logger.finest(nullSafeToString(o));
        }

        public static void debug(Logger logger, String msg, Object... objects) {
            if (logLevel.intValue() <= FINER_VALUE) {
                logger.finer(doSubstitutions(msg, objects));
            }
        }

        public static void debug(Logger logger, Object o) {
            logger.finer(nullSafeToString(o));
        }

        public static void info(Logger logger, String msg, Object... objects) {
            if (logLevel.intValue() <= INFO_VALUE) {
                logger.info(doSubstitutions(msg, objects));
            }
        }

        public static void info(Logger logger, Object o) {
            logger.info(nullSafeToString(o));
        }

        public static void warn(Logger logger, String msg, Object... objects) {
            if (logLevel.intValue() <= WARNING_VALUE) {
                logger.warning(doSubstitutions(msg, objects));
            }
        }

        public static void warn(Logger logger, Object o) {
            logger.warning(nullSafeToString(o));
        }

        public static void error(Logger logger, String msg, Object... objects) {
            if (logLevel.intValue() <= SEVERE_VALUE) {
                logger.severe(doSubstitutions(msg, objects));
            }
        }

        public static void error(Logger logger, String msg, Throwable thrown) {
            if (logLevel.intValue() <= SEVERE_VALUE) {
                msg = msg + '\n' + msgFromThrowable(thrown);
                logger.log(Level.SEVERE, msg, thrown);
            }
        }

        public static void error(Logger logger, Object o) {
            logger.severe(nullSafeToString(o));
        }

        public static void fatal(Logger logger, String msg, Object... objects) {
            String subd = doSubstitutions(msg, objects);
            logger.severe(subd);
            raiseCompilerError(subd);
        }

        public static void fatal(Logger logger, String msg, Throwable thrown) {
            msg = msg + '\n' + msgFromThrowable(thrown);
            logger.log(Level.SEVERE, msg, thrown);
            raiseCompilerError(msg);
        }

        public static void fatal(Logger logger, Object o) {
            String oString = nullSafeToString(o);
            logger.severe(oString);
            raiseCompilerError(oString);
        }

        /**
         * Get a message representing the given throwable, suitable for printing.
         * @return A String that summarises the given throwable.
         */
        private static String msgFromThrowable(Throwable t) {
            if (t == null) {
                return "";
            }

            final StackTraceElement[] stackTrace = t.getStackTrace();
            String msg = "\n\n" + t.getClass().getSimpleName() + ':' + t.getMessage() + '\n' + Arrays.toString(stackTrace).replace(',', '\n').replace('[', ' ').replace(']', ' ');

            if (t.getCause() != null) {
                return "\n    Caused by :" + msgFromThrowable(t.getCause());
            }

            return msg;
        }
    }
}
