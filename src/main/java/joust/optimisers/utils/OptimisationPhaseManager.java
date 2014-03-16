package joust.optimisers.utils;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import joust.optimisers.runnables.OptimisationRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.HashMap;
import java.util.LinkedList;

import static com.sun.source.util.TaskEvent.Kind;

/**
 * A runnable that will be executed by the compiler during the specified phase.
 * When to run is specified by both a Phase and PhaseModifier.
 */
public abstract class OptimisationPhaseManager implements Runnable {
    private static final HashMap<Kind, LinkedList<OptimisationRunnable>> tasksBefore = new HashMap<>();
    private static final HashMap<Kind, LinkedList<OptimisationRunnable>> tasksAfter = new HashMap<>();

    public static CompilerPhase currentPhase = CompilerPhase.fromKind(Kind.ANNOTATION_PROCESSING);

    public static enum PhaseModifier {
        BEFORE,
        AFTER
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

        TaskListener listener = new TaskListener() {
            Logger l = LogManager.getLogger();

            @Override
            public void finished(TaskEvent taskEvent) {
                l.debug("finished event: {}", taskEvent);
                runTasks(tasksAfter.get(taskEvent.getKind()));
            }

            @Override
            public void started(TaskEvent taskEvent) {
                l.debug("started event: {}", taskEvent);
                currentPhase = CompilerPhase.fromKind(taskEvent.getKind());
                runTasks(tasksBefore.get(taskEvent.getKind()));
            }

            private void runTasks(LinkedList<OptimisationRunnable> runnables) {
                for (OptimisationRunnable r : runnables) {
                    l.debug("Running {}", r.getClass().getName());
                    r.run();
                }
            }
        };

        // Get a reference to the ongoing compilation task.
        JavacTask compilationTask = JavacTask.instance(env);

        // Register the listener.
        compilationTask.addTaskListener(listener);
    }

    public static void register(OptimisationRunnable task, PhaseModifier modifier, Kind runWhen) {
        if (runWhen == Kind.ENTER || runWhen == Kind.PARSE) {
            throw new UnsupportedOperationException("It is impossible to run a PhaseSpecific runnable during a phase which has concluded.");
        }

        HashMap<Kind, LinkedList<OptimisationRunnable>> tasks = modifier == PhaseModifier.BEFORE ? tasksBefore : tasksAfter;
        tasks.get(runWhen).add(task);
    }
}
