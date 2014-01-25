package joust.optimisers.avail;

import com.sun.tools.javac.util.Name;

import java.util.concurrent.atomic.AtomicInteger;

import static joust.Optimiser.names;

/**
 * Class for creating unique character strings for use as temporary variable names.
 */
public class NameFactory {
    // The next name to generate.
    private static AtomicInteger tempName = new AtomicInteger(-1);

    public static Name getName() {
        // You can't start a name with an integer in Java source... But the AST doesn't mind.
        return names.fromString(tempName.incrementAndGet() + "$JOUST$");
    }
}
