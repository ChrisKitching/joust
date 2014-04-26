package joust.utils.compiler;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import joust.optimisers.runnables.OptimisationRunnable;
import joust.tree.annotatedtree.AJCForest;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.sun.source.util.TaskEvent.Kind;

/**
 * A runnable that will be executed by the compiler during the specified phase.
 * When to run is specified by both a Phase and PhaseModifier.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class OptimisationPhaseManager implements Runnable {
    private static final Map<Kind, LinkedList<OptimisationRunnable>> tasksBefore = new EnumMap<Kind, LinkedList<OptimisationRunnable>>(Kind.class);
    private static final Map<Kind, LinkedList<OptimisationRunnable>> tasksAfter = new EnumMap<Kind, LinkedList<OptimisationRunnable>>(Kind.class);
    // Records which trees are yet to have completed which events. We delay the dispatch of the "after" event until all
    // trees have processed that event.
    private static final Map<Kind, Set<CompilationUnitTree>> todo = new EnumMap<Kind, Set<CompilationUnitTree>>(Kind.class);

    private static final Map<VirtualPhase, LinkedList<OptimisationRunnable>> tasksBeforeVirtual = new EnumMap<VirtualPhase, LinkedList<OptimisationRunnable>>(VirtualPhase.class);
    private static final Map<VirtualPhase, LinkedList<OptimisationRunnable>> tasksAfterVirtual = new EnumMap<VirtualPhase, LinkedList<OptimisationRunnable>>(VirtualPhase.class);

    public static CompilerPhase currentPhase = CompilerPhase.fromKind(Kind.ANNOTATION_PROCESSING);

    public static boolean isAborting;

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
        runTasks(toRun, null); // TODO: Replace `null` with some shiny new toy if required...
    }

    public static void afterVirtual(VirtualPhase p) {
        LinkedList<OptimisationRunnable> toRun = tasksAfterVirtual.get(p);
        log.trace("Finished virtual event: {}", p);
        runTasks(toRun, null);
    }

    /**
     * Prepare the OptimisationPhaseManager class for use. Registers the actual listeners into javac.
     * The listeners will run all PhaseSpecificRunnables registered for each event.
     *
     * @param env The ProcessingEnvironment to which the listeners should associate.
     */
    public static void init(final ProcessingEnvironment env) {
        isAborting = false;

        tasksBefore.clear();
        tasksAfter.clear();
        todo.clear();
        tasksBeforeVirtual.clear();
        tasksAfterVirtual.clear();

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
                log.trace("finished event: {}", taskEvent);
                Set<CompilationUnitTree> todoHere = todo.get(taskEvent.getKind());
                if (todoHere != null) {
                    todoHere.remove(taskEvent.getCompilationUnit());
                    if (!todoHere.isEmpty()) {
                        return;
                    }
                }

                LinkedList<OptimisationRunnable> toRun = tasksAfter.get(taskEvent.getKind());
                runTasks(toRun, taskEvent);
            }

            @Override
            public void started(TaskEvent taskEvent) {
                log.trace("started event: {}", taskEvent);
                if (AJCForest.getInstance() != null) {
                    if (todo.containsKey(taskEvent.getKind())) {
                        return;
                    }

                    HashSet<CompilationUnitTree> trees = new HashSet<CompilationUnitTree>();
                    for (Env<AttrContext> env : AJCForest.getInstance().rootEnvironments.values()) {
                        trees.add(env.toplevel);
                    }
                    todo.put(taskEvent.getKind(), trees);
                }

                LinkedList<OptimisationRunnable> toRun = tasksBefore.get(taskEvent.getKind());
                runTasks(toRun, taskEvent);

                currentPhase = CompilerPhase.fromKind(taskEvent.getKind());
            }
        };

        // Get a reference to the ongoing compilation task.
        JavacTask compilationTask = JavacTask.instance(env);

        // Register the listener.
        compilationTask.addTaskListener(listener);
    }

    public static void runTasks(LinkedList<OptimisationRunnable> runnables, TaskEvent taskEvent) {
        for (OptimisationRunnable r : runnables) {
            if (isAborting) {
                return;
            }

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

    /**
     * Prevent all further optimisation phases from running. Called in the event of a fatal error.
     */
    public static void abort() {
        isAborting = true;

        tasksBefore.clear();
        tasksAfter.clear();
        tasksBeforeVirtual.clear();
        tasksAfterVirtual.clear();
    }
}
