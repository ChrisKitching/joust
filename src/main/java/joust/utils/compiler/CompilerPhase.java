package joust.utils.compiler;

import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.Logger;

import static com.sun.source.util.TaskEvent.Kind;

/**
 * Class representing a phase in the compiler pipeline.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class CompilerPhase {
    private static final PhaseKindComparator comparator = new PhaseKindComparator();

    // The entry from the TaskEvent.Kind enum describing this phase.
    private Kind javacKind;

    // Cache of CompilerPhase objects...
    private static HashMap<Kind, CompilerPhase> objectCache = new HashMap<Kind, CompilerPhase>();

    /**
     * Get an object representing the given javac TaskEvent.Kind.
     */
    public static CompilerPhase fromKind(Kind k) {
        k = ignoreAnnotationProcessingRounds(k);
        CompilerPhase phase = objectCache.get(k);

        if (phase == null) {
            phase = new CompilerPhase(k);
            objectCache.put(k, phase);
        }

        return phase;
    }

    private CompilerPhase(Kind k) {
        javacKind = k;
    }

    public boolean isBefore(CompilerPhase phase) {
        return comparator.compare(javacKind, phase.javacKind) == 1;
    }
    public boolean isBefore(Kind phase) {
        return isBefore(new CompilerPhase(phase));
    }

    public boolean isAfter(CompilerPhase phase) {
        return comparator.compare(javacKind, phase.javacKind) == 0;
    }
    public boolean isAfter(Kind phase) {
        return isBefore(new CompilerPhase(phase));
    }

    protected static Kind ignoreAnnotationProcessingRounds(Kind k) {
        if (k == Kind.ANNOTATION_PROCESSING_ROUND) {
            k = Kind.ANNOTATION_PROCESSING;
        }

        return k;
    }

    /**
     * A comparator for comparing javac phase Kind objects.
     * Just for fun, the javac enum holding these doesn't actually have them in order, so we can't cheat
     * and compare enum values.
     * Yay!
     */
    private static class PhaseKindComparator implements Comparator<Kind> {
        /**
         * Returns 1 if o1 comes before o2, returns -1 if o2 comes before o1, returns 0 otherwise.
         */
        @Override
        public int compare(Kind o1, Kind o2) {
            // Treat all annotation processing rounds equal...
            o1 = ignoreAnnotationProcessingRounds(o1);
            o2 = ignoreAnnotationProcessingRounds(o2);

            if (o1 == o2) {
                return 0;
            }

            // The end states...
            if (o1 == Kind.PARSE || o2 == Kind.GENERATE) {
                return 1;
            }

            if (o2 == Kind.PARSE || o1 == Kind.GENERATE) {
                return -1;
            }

            // Neither are PARSE or GENERATE (The phases at either end) - so check the new ends...
            if (o1 == Kind.ENTER || o2 == Kind.ANALYZE) {
                return 1;
            }

            if (o2 == Kind.ENTER || o1 == Kind.ANALYZE) {
                return -1;
            }

            // We can never get here...
            log.fatal("Impossible state encountered in PhaseKindComparator for: " + o1 + ", " + o2);
            return 0;
        }
    }

    @Override
    public String toString() {
        return javacKind.toString();
    }
}
