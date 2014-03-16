package tests.unittests;

import joust.OptimiserOptions;
import org.junit.Test;

import static org.junit.Assert.*;


public class OptimiserOptionsTest {
    @Test
    public void testConfigureFromArgumentArray() {
        String[] testArgs = {};
        assertTrue(OptimiserOptions.configureFromArgumentArray(testArgs));
        assertFalse(OptimiserOptions.stripAssertions);

        // Alas, the use of enums in annotations is unhelpful for our purposes... :(
        testArgs = new String[] {"--strip-assertions"};
        assertTrue(OptimiserOptions.configureFromArgumentArray(testArgs));
        assertTrue(OptimiserOptions.stripAssertions);

        testArgs = new String[] {"IT'S", "TOASTER", "TIME"};
        assertFalse(OptimiserOptions.configureFromArgumentArray(testArgs));

        testArgs = new String[] {"-a"};
        assertTrue(OptimiserOptions.configureFromArgumentArray(testArgs));
        assertTrue(OptimiserOptions.stripAssertions);

        testArgs = new String[] {"-h"};
        assertFalse(OptimiserOptions.configureFromArgumentArray(testArgs));

        testArgs = new String[] {"--help"};
        assertFalse(OptimiserOptions.configureFromArgumentArray(testArgs));
    }
}
