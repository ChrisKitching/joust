package joust;

import com.lexicalscope.jewel.cli.Option;

/**
 * Interface to provide a target for the JewelCLI API to attach command line arguments to.
 */
public interface CLITarget {
    @Option(helpRequest = true, longName = "help", shortName = "h", description = "Display this help message")
    boolean getHelp();

    @Option(longName = "strip-assertions", shortName = "a", description = "Remove all assertion statements from target.")
    boolean getStripAssertions();
}
