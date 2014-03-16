package joust;

import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.comp.Todo;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import joust.optimisers.runnables.OptimisationRunnable;
import joust.optimisers.utils.OptimisationPhaseManager;
import joust.utils.LogUtils;
import joust.utils.StaticCompilerUtils;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;

import static joust.utils.StaticCompilerUtils.javaCompiler;
import static joust.optimisers.utils.OptimisationPhaseManager.VirtualPhase.*;

/**
 * The class we don't like to talk about. Nothing to see here.
 */
@Log4j2
public final class JavacBrutaliser extends OptimisationRunnable {
    private static final String FAILED_TO_SET_COMPILE_POLICY = "Unable to hack compiler and set compile policy to simple. Please recompile with undocumented option 'compilePolicy' set to 'simple'. Assuming it still exists, that is.";
    private static final String FAILED_TO_BRUTALISE_COMPILER = "Unable to convince compiler to run some phases earlier than scheduled. Please recompile with a compiler that meets the undocumented binary compatability requirements for JOUST.";

    /**
     * Beat javac with a large stick until it desugars everything for us.
     * Essentially, what we're doing is:
     * desugar(flow(attribute(todo)))
     *
     * as per the comparable line in JavaCompiler.java:compile2.
     *
     * Sucks that the event system doesn't have a listener for after desugaring... :(.
     *
     * The difference of course being that we're doing this from annotation processing land. This is okay, since
     * the very next thing javac is going to do after annotation processing is call compile2. Provided we call this
     * at the very end of the very final annotation processing round, the world will probably not end.
     */
    static void shortCircuitCompiler() {
        log.info("Beating javac with a big stick...");

        if (!hijackCompilePolicy()) {
            LogUtils.raiseCompilerError(FAILED_TO_SET_COMPILE_POLICY);
            return;
        }

        Class<JavaCompiler> jCompilerClass = JavaCompiler.class;

        Method attributeMethod;
        Method flowMethod;
        Method desugarMethod;
        Method generateMethod;
        try {
            attributeMethod = jCompilerClass.getDeclaredMethod("attribute", Queue.class);
            attributeMethod.setAccessible(true);

            flowMethod = jCompilerClass.getDeclaredMethod("flow", Queue.class);
            flowMethod.setAccessible(true);

            desugarMethod = jCompilerClass.getDeclaredMethod("desugar", Queue.class);
            desugarMethod.setAccessible(true);

            generateMethod = jCompilerClass.getDeclaredMethod("generate", Queue.class);
            generateMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            log.error("Unable to find phase method on JavaCompiler.", e);
            LogUtils.raiseCompilerError(FAILED_TO_BRUTALISE_COMPILER);
            return;
        }

        Todo todoValue = javaCompiler.todo;

        log.info("todo: {}", todoValue);

        try {
            log.info("Entering attribution...");
            Object attributed = attributeMethod.invoke(javaCompiler, todoValue);
            log.info("Attribution complete.");
            log.info("Entering flow...");
            Object flowed = flowMethod.invoke(javaCompiler, attributed);
            log.info("Flow complete.");

            log.info("Entering desugar...");
            OptimisationPhaseManager.beforeVirtual(DESUGAR);

            Object desugared = desugarMethod.invoke(javaCompiler, flowed);

            OptimisationPhaseManager.afterVirtual(DESUGAR);
            log.info("Desugar complete.");

            // Finish up..
            log.info("Entering generate...");
            generateMethod.invoke(javaCompiler, desugared);
            log.info("Generate complete.");

            log.info("todo: {}", todoValue);
            log.info("Resources released.");
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Exception thrown while calling extra compiler phases (Now *there's* a surprise).", e);
            LogUtils.raiseCompilerError(FAILED_TO_BRUTALISE_COMPILER);
            return;
        }
        log.info("Success.");
    }

    /**
     * Switch javac's compile policy for compile2 to "simple". Harms memory performance, but
     * makes inter-compilation-unit optimisations easier (by ensuring each pass of the compiler
     * is applied to every unit before the next one starts).
     * @return true if the mutulation is a success, false if something failed.
     */
    private static boolean hijackCompilePolicy() {
        log.info("Hijacking compile policy...");
        Class<JavaCompiler> jCompilerClass = JavaCompiler.class;

        // Accessing non-public nested classes must unfortunately be done in this way.
        Class<?>[] containedClasses = jCompilerClass.getDeclaredClasses();

        log.debug("Searching for CompilePolicy class...");
        // Find the class for JavaCompiler.CompilePolicy.
        Class<?> compilePolicyClass = null;
        for (Class<?> c : containedClasses) {
            if ("com.sun.tools.javac.main.JavaCompiler$CompilePolicy".equals(c.getName())) {
                log.debug("Found: {}", c.getCanonicalName());
                compilePolicyClass = c;
            }
        }

        if (compilePolicyClass == null) {
            log.error("Unable to find JavaCompiler.CompilePolicy class.");
            return false;
        }

        try {
            Method decodeMethod = compilePolicyClass.getDeclaredMethod("decode", String.class);
            decodeMethod.setAccessible(true);

            Object compilePolicy = decodeMethod.invoke(null, "simple");

            Field policyField = jCompilerClass.getDeclaredField("compilePolicy");
            policyField.setAccessible(true);
            log.debug("Old value: {} New value:{}", policyField.get(javaCompiler), compilePolicy);
            policyField.set(javaCompiler, compilePolicy);

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
            log.error("Failed to hack javac's compile policy:\n {}", e);
            return false;
        }

        log.info("Success.");
        return true;
    }

    @Override
    public void run() {
        // We're invoked after annotation processing. The final compiler has been created and tacked onto a
        // context but hasn't been returned and assigned to the first compiler's delegateCompiler field yet.
        Context finalContext = Optimiser.environ.getContext();
        StaticCompilerUtils.uninit();
        StaticCompilerUtils.initWithContext(finalContext);

        List<JCTree.JCCompilationUnit> compilationUnitList = List.nil();

        for (JCTree.JCCompilationUnit unit : Optimiser.conventionalTrees) {
            compilationUnitList = compilationUnitList.append(unit);
        }

        javaCompiler.enterTreesIfNeeded(compilationUnitList);

        shortCircuitCompiler();

        /*
         * Since we're taking control of the compilation phases after annotation processing by ourselves, we want
         * the compiler to not attempt to do those phases itself when it returns.
         * So, we take this opportunity to bludgeon the appropriate undocumented option into javac.
         */
        javaCompiler.shouldStopPolicyIfNoError = CompileStates.CompileState.INIT;

        log.error(javaCompiler.todo);
    }
}
