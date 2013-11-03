package joust.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

public class LogUtils {
    private static ProcessingEnvironment processingEnv;

    public static void init(ProcessingEnvironment env) {
        processingEnv = env;
    }

    /**
     * Raises a fatal compiler error with the given message, halting execution.
     * TODO: Consider refactoring as log4j Appender.
     *
     * @param error The error string to display.
     */
    public static void raiseCompilerError(String error) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Fatal error from optimiser: "+error);
    }
}
