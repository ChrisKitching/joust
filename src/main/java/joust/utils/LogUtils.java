package joust.utils;

import lombok.extern.log4j.Log4j2;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

public @Log4j2
class LogUtils {
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
        log.error("Fatal error from optimiser: {}", error);
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Fatal error from optimiser: "+error);
    }
}
