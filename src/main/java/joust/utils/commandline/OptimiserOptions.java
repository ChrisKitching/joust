package joust.utils.commandline;

import joust.optimisers.cse.CommonSubExpressionTranslator;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to contain the options passed to the optimiser on the command line.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class OptimiserOptions {
    // If true, assertions found in the input program are deleted.
    public static boolean stripAssertions;
    public static boolean annotatingLibrary;
    public static boolean dumpingEffectKeys;

    public static Level logLevel = Level.INFO;

    /**
     * Configure the OptimiserOptions from the given processing environment's command line arguments.
     *
     * @param env The environment from which to draw command line arguments.
     * @return true if parsing was a success and the options are valid, false if the program should
     *         now abort.
     */
    public static boolean configureFromProcessingEnvironment(ProcessingEnvironment env) {
        Map<String, String> args = env.getOptions();
        log.error("KeySet: {}", Arrays.toString(args.keySet().toArray()));

        String loggingLevel = args.get("JOUSTLogLevel");
        log.error("Got log level: {}", loggingLevel);
        if (loggingLevel != null) {
            switch (loggingLevel) {
                case "SEVERE":
                    logLevel = Level.SEVERE;
                    break;
                case "WARNING":
                    logLevel = Level.WARNING;
                    break;
                case "INFO":
                    logLevel = Level.INFO;
                    break;
                case "CONFIG":
                    logLevel = Level.CONFIG;
                    break;
                case "FINE":
                    logLevel = Level.FINE;
                    break;
                case "FINER":
                    logLevel = Level.FINER;
                    break;
                case "FINEST":
                    logLevel = Level.FINEST;
                    break;
                case "ALL":
                    logLevel = Level.ALL;
                    break;
                case "OFF":
                    logLevel = Level.OFF;
                    break;
                default:
                    return false;
            }
        }

        stripAssertions = args.containsKey("JOUSTStripAssertions");
        annotatingLibrary = args.containsKey("JOUSTAnnotateLib");
        dumpingEffectKeys = args.containsKey("JOUSTPrintEffectCacheKeys");

        if (args.containsKey("JOUSTMinCSEScore")) {
            CommonSubExpressionTranslator.MINIMUM_CSE_SCORE = Integer.parseInt(args.get("JOUSTMinCSEScore"));
        }

        return true;
    }
}
