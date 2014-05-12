package joust.utils.compiler;

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Todo;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;
import joust.JOUST;
import joust.optimisers.runnables.OptimisationRunnable;
import joust.utils.ReflectionUtils;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;
import static joust.utils.ReflectionUtils.*;
import static joust.utils.compiler.StaticCompilerUtils.javaCompiler;
import static joust.utils.compiler.OptimisationPhaseManager.EventType.*;

/**
 * The class we don't like to talk about. Nothing to see here.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public final class JavacBrutaliser extends OptimisationRunnable {
    /**
     * Convince javac to desugar everything for us.
     * Essentially, what we're doing is:
     * desugar(flow(attribute(todo)))
     * as per the comparable line in JavaCompiler.java:compile2.
     *
     * The difference of course being that we're doing this from
     * annotation processing land. This is okay, since
     * the very next thing javac is going to do after annotation
     * processing is call compile2. Provided we call this at the very end
     * of the very final annotation processing round and have javac exit
     * immediately from compile2, the world will probably not end.
     */
    static void shortCircuitCompiler() {
        Class<JavaCompiler> jCompilerClass = JavaCompiler.class;

        // Obtain reflective access to the methods called in compile2
        Method attributeMethod =
            getAccessibleMethod(jCompilerClass, "attribute", Queue.class);
        Method flowMethod =
            getAccessibleMethod(jCompilerClass, "flow", Queue.class);
        Method desugarMethod =
            getAccessibleMethod(jCompilerClass, "desugar", Queue.class);
        Method generateMethod =
            getAccessibleMethod(jCompilerClass, "generate", Queue.class);

        try {
            log.info("Attribution.");
            Object attributed = attributeMethod.invoke(javaCompiler, javaCompiler.todo);
            OptimisationPhaseManager.dispatchEvent(AFTER_ATTRIBUTION);

            log.info("Flow.");
            Object flowed = flowMethod.invoke(javaCompiler, attributed);
            OptimisationPhaseManager.dispatchEvent(AFTER_FLOW);

            log.info("Desugar.");
            Object desugared = desugarMethod.invoke(javaCompiler, flowed);

            // Capture the environments with attribution results for JOUST.
            JOUST.environmentsToProcess =
                (Queue<Pair<Env<AttrContext>, JCClassDecl>>) desugared;
            OptimisationPhaseManager.dispatchEvent(AFTER_DESUGAR);

            log.info("Generate.");
            generateMethod.invoke(javaCompiler, desugared);
            OptimisationPhaseManager.dispatchEvent(AFTER_GENERATE);

            log.info("Done.");
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.fatal("Unable to run compiler phases early!", e);
        }
    }

    @Override
    public void run() {
        // Get the post-annotation-processing context (javac sometimes
        // reassigns it during annotation processing.)
        Context finalContext = JOUST.environ.getContext();
        StaticCompilerUtils.uninit();
        StaticCompilerUtils.initWithContext(finalContext);

        List<JCCompilationUnit> compilationUnitList = List.nil();
        for (JCCompilationUnit unit : JOUST.conventionalTrees) {
            compilationUnitList = compilationUnitList.append(unit);
        }

        // Ensure all trees in this job are on the todo list.
        javaCompiler.enterTreesIfNeeded(compilationUnitList);

        shortCircuitCompiler();

        // Since we're taking control of the compilation phases after
        // annotation processing by ourselves, we want the compiler to not
        // attempt to run those phases itself when it returns. Setting this
        // causes the compiler the exist immediately after it finishes
        // annotation processing.
        javaCompiler.shouldStopPolicyIfNoError = CompileStates.CompileState.INIT;
    }
}
