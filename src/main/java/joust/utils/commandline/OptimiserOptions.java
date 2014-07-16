package joust.utils.commandline;

import joust.optimisers.cse.CommonSubExpressionTranslator;
import joust.optimisers.runnables.OptimisationRunnable;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Class to contain the options passed to the optimiser on the command line.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class OptimiserOptions {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[ ]*,[ ]*");
    // If true, assertions found in the input program are deleted.
    public static boolean stripAssertions;
    public static boolean annotatingLibrary;
    public static boolean dumpingEffectKeys;

    public static Level logLevel = Level.INFO;

    // Optimisations explicitly enabled by an argument.
    private static HashSet<String> enabledOptimisations;

    // Optimisations explicitly disabled by an argument. Only one of these two collections may be
    // present.
    private static HashSet<String> disabledOptimisations;

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
            if ("SEVERE".equals(loggingLevel)) {
                logLevel = Level.SEVERE;
            } else if ("WARNING".equals(loggingLevel)) {
                logLevel = Level.WARNING;
            } else if ("INFO".equals(loggingLevel)) {
                logLevel = Level.INFO;
            } else if ("CONFIG".equals(loggingLevel)) {
                logLevel = Level.CONFIG;
            } else if ("FINE".equals(loggingLevel)) {
                logLevel = Level.FINE;
            } else if ("FINER".equals(loggingLevel)) {
                logLevel = Level.FINER;
            } else if ("FINEST".equals(loggingLevel)) {
                logLevel = Level.FINEST;
            } else if ("ALL".equals(loggingLevel)) {
                logLevel = Level.ALL;
            } else if ("OFF".equals(loggingLevel)) {
                logLevel = Level.OFF;
            } else {
                return false;
            }
        }

        stripAssertions = args.containsKey("JOUSTStripAssertions");
        annotatingLibrary = args.containsKey("JOUSTAnnotateLib");
        dumpingEffectKeys = args.containsKey("JOUSTPrintEffectCacheKeys");

        if (args.containsKey("JOUSTMinCSEScore")) {
            CommonSubExpressionTranslator.MINIMUM_CSE_SCORE = Integer.parseInt(args.get("JOUSTMinCSEScore"));
        }

        // Detect enabled optimisations.
        String enabled = args.get("JOUSTEnabledOptimisations");
        String disabled = args.get("JOUSTDisabledOptimisations");

        if (enabled != null && disabled != null) {
            // Invalid configuration!
            log.error("Cannot specify both JOUSTEnabledOptimisations and JOUSTDisabledOptimisations!");
        }

        if (enabled != null) {
            enabledOptimisations = new HashSet<String>();
            enabledOptimisations.add(OptimisationRunnable.NAME_CORE);
            configureFromString(enabled, enabledOptimisations);
        } else if (disabled != null) {
            disabledOptimisations = new HashSet<String>();
            configureFromString(disabled, disabledOptimisations);
        }

        return true;
    }

    private static void configureFromString(String enabled, Set<String> targetSet) {
        String[] args = SPLIT_PATTERN.split(enabled);
        for (int i = 0; i < args.length; i++) {
            targetSet.add(args[i]);
        }

        log.error("Enabled: {}", Arrays.toString(args));
    }

    /**
     * Determine if, given the various enable/disable options, a given optimisation should be run.
     */
    public static boolean shouldRun(OptimisationRunnable task) {
        if (enabledOptimisations != null) {
            return enabledOptimisations.contains(task.getName());
        }

        if (disabledOptimisations != null) {
            return !disabledOptimisations.contains(task.getName());
        }

        return true;
    }
}
