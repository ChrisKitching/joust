package joust.utils.commandline;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class to contain the options passed to the optimiser on the command line.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class OptimiserOptions {
    // If true, assertions found in the input program are deleted.
    public static boolean stripAssertions;

    /**
     * Configure the OptimiserOptions from the given processing environment's command line arguments.
     *
     * @param env The environment from which to draw command line arguments.
     * @return true if parsing was a success and the options are valid, false if the program should
     *         now abort.
     */
    public static boolean configureFromProcessingEnvironment(ProcessingEnvironment env) {
        Map<String, String> args = env.getOptions();
        String argString = args.get("joustOptions");

        // No arguments given.
        if (argString == null) {
            return true;
        }

        return configureFromArgumentArray( argString.split(" "));
    }

    /**
     * Configure the OptimiserOptions from the given argument array.
     *
     * @param argsArray The array of command line arguments to parse.
     * @return true if parsing was a success and the options are valid, false if the program should
     *         now abort.
     */
    public static boolean configureFromArgumentArray(String[] argsArray) {
        log.debug("Args: " + Arrays.toString(argsArray));

        CLITarget result;
        try {
            result = CliFactory.parseArguments(CLITarget.class, argsArray);
        } catch (HelpRequestedException e) {
            log.fatal(e.getMessage());
            return false;
        } catch (ArgumentValidationException e) {
            log.fatal("Argument validation exception:\n", e.getMessage());
            return false;
        }

        configureFromCLITarget(result);

        return true;
    }

    /**
     * Configure the OptimiserOptions from a CLITarget object.
     *
     * @param target The CLITarget object to process
     */
    public static void configureFromCLITarget(CLITarget target) {
        stripAssertions = target.getStripAssertions();
    }
}
