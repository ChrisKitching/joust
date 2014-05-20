package joust.utils.logging;

import joust.utils.commandline.OptimiserOptions;
import joust.utils.compiler.OptimisationPhaseManager;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private static BufferedOutputStream fos;

    public static void init(ProcessingEnvironment env) {
        processingEnv = env;
        // Since the Ansi library doesn't understand zsh...
        System.setProperty("Ansi", "true");
        Logger logger = LogManager.getLogManager().getLogger("");
        logger.setLevel(logLevel);
        Handler handler = logger.getHandlers()[0];
        handler.setLevel(logLevel);
        handler.setFormatter(new LogFormatter());

        String homeDirectory = System.getProperty("user.home");
        File joustDir = new File(homeDirectory + "/.joust/");
        joustDir.mkdirs();

        File logFile = new File(joustDir + "/.JOUSTLOG");

        try {
            fos = new BufferedOutputStream(new FileOutputStream(logFile, true), 8192);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void unInit() {
        try {
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fos = null;
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
                String sMsg = doSubstitutions(msg, objects);
                logToFile(sMsg);
                logger.finest(sMsg);
            }
        }

        public static void trace(Logger logger, Object o) {
            String msg = nullSafeToString(o);
            logToFile(msg);
            logger.finest(msg);
        }

        public static void debug(Logger logger, String msg, Object... objects) {
            if (logLevel.intValue() <= FINER_VALUE) {
                String sMsg = doSubstitutions(msg, objects);
                logToFile(sMsg);
                logger.finer(sMsg);
            }
        }

        public static void debug(Logger logger, Object o) {
            String msg = nullSafeToString(o);
            logToFile(msg);
            logger.finer(msg);
        }

        public static void info(Logger logger, String msg, Object... objects) {
            if (logLevel.intValue() <= INFO_VALUE) {
                String sMsg = doSubstitutions(msg, objects);
                logToFile(sMsg);
                logger.info(sMsg);
            }
        }

        private static void logToFile(String msg) {
            if (fos == null) {
                return;
            }

            try {
                fos.write((msg + '\n').getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void info(Logger logger, Object o) {
            String msg = nullSafeToString(o);
            logToFile(msg);
            logger.info(msg);
        }

        public static void warn(Logger logger, String msg, Object... objects) {
            if (logLevel.intValue() <= WARNING_VALUE) {
                String sMsg = doSubstitutions(msg, objects);
                logToFile(sMsg);
                logger.warning(sMsg);
            }
        }

        public static void warn(Logger logger, Object o) {
            String msg = nullSafeToString(o);
            logToFile(msg);
            logger.warning(msg);
        }

        public static void error(Logger logger, String msg, Object... objects) {
            if (logLevel.intValue() <= SEVERE_VALUE) {
                String sMsg = doSubstitutions(msg, objects);
                logToFile(sMsg);
                logger.severe(sMsg);
            }
        }

        public static void error(Logger logger, String msg, Throwable thrown) {
            if (logLevel.intValue() <= SEVERE_VALUE) {
                msg = msg + '\n' + msgFromThrowable(thrown);
                logToFile(msg);
                logger.log(Level.SEVERE, msg, thrown);
            }
        }

        public static void error(Logger logger, Object o) {
            String msg = nullSafeToString(o);
            logToFile(msg);
            logger.severe(msg);
        }

        public static void fatal(Logger logger, String msg, Object... objects) {
            OptimisationPhaseManager.abort();
            String subd = doSubstitutions(msg, objects);
            logToFile(subd);
            logger.severe(subd);
            raiseCompilerError(subd);
        }

        public static void fatal(Logger logger, String msg, Throwable thrown) {
            OptimisationPhaseManager.abort();
            msg = msg + '\n' + msgFromThrowable(thrown);
            logToFile(msg);
            logger.log(Level.SEVERE, msg, thrown);
            raiseCompilerError(msg);
        }

        public static void fatal(Logger logger, Object o) {
            OptimisationPhaseManager.abort();
            String oString = nullSafeToString(o);
            logToFile(oString);
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
