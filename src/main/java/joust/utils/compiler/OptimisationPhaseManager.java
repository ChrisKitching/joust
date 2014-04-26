package joust.utils.compiler;

import joust.optimisers.runnables.OptimisationRunnable;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A runnable that will be executed by the compiler during the specified phase.
 * When to run is specified by both a Phase and PhaseModifier.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class OptimisationPhaseManager implements Runnable {
    private static final Map<EventType, LinkedList<OptimisationRunnable>> eventMappings = new EnumMap<EventType, LinkedList<OptimisationRunnable>>(EventType.class);

    public static boolean isAborting;

    /**
     * Phases that javac does not support, but that the JOUST framework adds via magic.
     */
    public enum EventType {
        AFTER_ANNOTATION_PROCESSING,
        AFTER_ATTRIBUTION,
        AFTER_FLOW,
        AFTER_DESUGAR,
        AFTER_GENERATE
    }

    public static void dispatchEvent(EventType p) {
        LinkedList<OptimisationRunnable> toRun = eventMappings.get(p);
        log.trace("Finished virtual event: {}", p);
        runTasks(toRun);
    }

    /**
     * Prepare the OptimisationPhaseManager class for use. Registers the actual listeners into javac.
     * The listeners will run all PhaseSpecificRunnables registered for each event.
     *
     */
    public static void init() {
        isAborting = false;

        eventMappings.clear();

        // Initialise the HashMaps with empty lists.
        for (EventType p : EventType.values()) {
            eventMappings.put(p, new LinkedList<OptimisationRunnable>());
        }
    }

    public static void runTasks(LinkedList<OptimisationRunnable> runnables) {
        for (OptimisationRunnable r : runnables) {
            if (isAborting) {
                return;
            }

            log.trace("Running {}", r.getClass().getName());

            r.run();
        }
    }

    public static void register(OptimisationRunnable task, EventType runWhen) {
        Map<EventType, LinkedList<OptimisationRunnable>> tasks =
                eventMappings;
        tasks.get(runWhen).add(task);
    }

    /**
     * Prevent all further optimisation phases from running. Called in the event of a fatal error.
     */
    public static void abort() {
        isAborting = true;

        eventMappings.clear();
    }
}
