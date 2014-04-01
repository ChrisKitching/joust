package joust.optimisers.utils;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import joust.optimisers.runnables.OptimisationRunnable;
import joust.utils.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static com.sun.source.util.TaskEvent.Kind;

/**
 * A runnable that will be executed by the compiler during the specified phase.
 * When to run is specified by both a Phase and PhaseModifier.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class OptimisationPhaseManager implements Runnable {
    private static final Map<Kind, LinkedList<OptimisationRunnable>> tasksBefore = new EnumMap<>(Kind.class);
    private static final Map<Kind, LinkedList<OptimisationRunnable>> tasksAfter = new EnumMap<>(Kind.class);

    private static final Map<VirtualPhase, LinkedList<OptimisationRunnable>> tasksBeforeVirtual = new EnumMap<>(VirtualPhase.class);
    private static final Map<VirtualPhase, LinkedList<OptimisationRunnable>> tasksAfterVirtual = new EnumMap<>(VirtualPhase.class);

    public static CompilerPhase currentPhase = CompilerPhase.fromKind(Kind.ANNOTATION_PROCESSING);

    public enum PhaseModifier {
        BEFORE,
        AFTER
    }

    /**
     * Phases that javac does not support, but that the JOUST framework adds via magic.
     */
    public enum VirtualPhase {
        DESUGAR
    }

    public static void beforeVirtual(VirtualPhase p) {
        LinkedList<OptimisationRunnable> toRun = tasksBeforeVirtual.get(p);
        log.trace("Started virtual event: {}", p);
        runTasks(toRun);
    }

    public static void afterVirtual(VirtualPhase p) {
        LinkedList<OptimisationRunnable> toRun = tasksAfterVirtual.get(p);
        log.trace("Finished virtual event: {}", p);
        runTasks(toRun);
    }

    /**
     * Prepare the OptimisationPhaseManager class for use. Registers the actual listeners into javac.
     * The listeners will run all PhaseSpecificRunnables registered for each event.
     *
     * @param env The ProcessingEnvironment to which the listeners should associate.
     */
    public static void init(final ProcessingEnvironment env) {
        tasksBefore.clear();
        tasksAfter.clear();

        // Initialise the HashMaps with empty lists.
        for (Kind p : Kind.values()) {
            tasksBefore.put(p, new LinkedList<OptimisationRunnable>());
            tasksAfter.put(p, new LinkedList<OptimisationRunnable>());
        }
        for (VirtualPhase p : VirtualPhase.values()) {
            tasksBeforeVirtual.put(p, new LinkedList<OptimisationRunnable>());
            tasksAfterVirtual.put(p, new LinkedList<OptimisationRunnable>());
        }


        TaskListener listener = new TaskListener() {
            @Override
            public void finished(TaskEvent taskEvent) {
                LinkedList<OptimisationRunnable> toRun = tasksAfter.get(taskEvent.getKind());
                log.trace("finished event: {}", taskEvent);
                runTasks(toRun);
            }

            @Override
            public void started(TaskEvent taskEvent) {
                LinkedList<OptimisationRunnable> toRun = tasksBefore.get(taskEvent.getKind());
                log.trace("started event: {}", taskEvent);
                runTasks(toRun);

                currentPhase = CompilerPhase.fromKind(taskEvent.getKind());
            }
        };

        // Get a reference to the ongoing compilation task.
        JavacTask compilationTask = JavacTask.instance(env);

        // Register the listener.
        compilationTask.addTaskListener(listener);
    }

    public static void runTasks(LinkedList<OptimisationRunnable> runnables) {
        for (OptimisationRunnable r : runnables) {
            log.trace("Running {}", r.getClass().getName());
            r.run();
        }
    }

    public static void register(OptimisationRunnable task, PhaseModifier modifier, Kind runWhen) {
        if (runWhen == Kind.ENTER || runWhen == Kind.PARSE) {
            log.fatal("Error during JOUST startup.", new UnsupportedOperationException("It is impossible to run a PhaseSpecific runnable during a phase which has concluded."));
        }

        Map<Kind, LinkedList<OptimisationRunnable>> tasks = modifier == PhaseModifier.BEFORE ? tasksBefore : tasksAfter;
        tasks.get(runWhen).add(task);
    }

    public static void register(OptimisationRunnable task, PhaseModifier modifier, VirtualPhase runWhen) {
        Map<VirtualPhase, LinkedList<OptimisationRunnable>> tasks = modifier == PhaseModifier.BEFORE ? tasksBeforeVirtual : tasksAfterVirtual;
        tasks.get(runWhen).add(task);
    }
}
