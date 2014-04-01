package joust;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import joust.optimisers.runnables.AssertionStrip;
import joust.optimisers.runnables.CSE;
import joust.optimisers.runnables.ConstFold;
import joust.optimisers.runnables.LoopInvar;
import joust.optimisers.runnables.TreeConverter;
import joust.optimisers.runnables.Unroll;
import joust.optimisers.utils.OptimisationPhaseManager;
import joust.tree.annotatedtree.AJCForest;
import joust.utils.LogUtils;
import joust.utils.StaticCompilerUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static joust.optimisers.utils.OptimisationPhaseManager.PhaseModifier.*;
import static joust.optimisers.utils.OptimisationPhaseManager.VirtualPhase.*;
import static com.sun.source.util.TaskEvent.Kind.*;
import static joust.utils.StaticCompilerUtils.javaElements;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("*")
public class Optimiser extends AbstractProcessor {
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s\n");
        System.setProperty("java.util.logging.ConsoleHandler.level", "FINEST");
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.ALL);
        Handler[] handlers = rootLogger.getHandlers();
        for(Handler h: handlers){
            h.setLevel(Level.ALL);
        }
    }

    public static AJCForest inputTrees;

    // The untranslated input JCTrees. The route to the AST prior to the desugaring step.
    public static LinkedHashSet<JCTree.JCCompilationUnit> conventionalTrees;

    public static JavacProcessingEnvironment environ;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        StaticCompilerUtils.uninit();
        AJCForest.uninit();

        environ = (JavacProcessingEnvironment) env;

        // So we can log those fatal errors...
        LogUtils.init(processingEnv);

        // Parse command line options.
        if (!OptimiserOptions.configureFromProcessingEnvironment(env)) {
            log.fatal("Optimiser aborted by command line argument processor.");
            return;
        }

        conventionalTrees = new LinkedHashSet<>();

        OptimisationPhaseManager.init(env);
        OptimisationPhaseManager.register(new JavacBrutaliser(), AFTER, ANNOTATION_PROCESSING);

        OptimisationPhaseManager.register(new AssertionStrip(), AFTER, ANNOTATION_PROCESSING);

        // As it happens, almost all our phases operate on the virtual AFTER DESUGAR phase (as this turns out to be
        // very much more convenient than working on the actual tree if you don't care about the desugared things.)
        OptimisationPhaseManager.register(new TreeConverter(), AFTER, DESUGAR);
        OptimisationPhaseManager.register(new ConstFold(), AFTER, DESUGAR);

        // TODO: Repair and re-enable this.
        // OptimisationPhaseManager.register(new AssignmentStrip(), AFTER, DESUGAR);
        OptimisationPhaseManager.register(new LoopInvar(), AFTER, DESUGAR);
        OptimisationPhaseManager.register(new Unroll(), AFTER, DESUGAR);
        OptimisationPhaseManager.register(new CSE(), AFTER, DESUGAR);

        // The post-compilation pass to populate the disk cache with the results of classes processed
        // during this job. Needs to happen here so we can compute a checksum over the bytecode and
        // spot when things get sneakily changed when we weren't looking.
        //OptimisationPhaseManager.register(new ChecksumRunner(), AFTER, GENERATE);

        StaticCompilerUtils.init(env);
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getRootElements()) {
            // Stash the root elements for later use...
            JCTree.JCCompilationUnit unit = javaElements.getTreeAndTopLevel(element, null, null).snd;

            conventionalTrees.add(unit);
        }

        return false;
    }
}
