package joust.utils;

import com.sun.tools.javac.util.Name;

import java.util.concurrent.atomic.AtomicInteger;

import static joust.utils.StaticCompilerUtils.*;

public final class NameFactory {
    // The next name to generate.
    private static final AtomicInteger tempName = new AtomicInteger(-1);

    public static Name getName() {
        // You can't start a name with an integer in Java source... But the AST doesn't mind.
        return names.fromString(tempName.incrementAndGet() + "$JOUST$");
    }
}
