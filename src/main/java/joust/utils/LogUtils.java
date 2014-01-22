package joust.utils;

import lombok.extern.log4j.Log4j2;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.util.Arrays;

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
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Fatal error from optimiser: " + error + "\nAt:\n" + Arrays.toString((new Exception().getStackTrace())).replace(',', '\n'));
        log.fatal("Fatal error from optimiser: {}", error);
    }
}
