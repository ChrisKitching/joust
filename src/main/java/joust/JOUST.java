package joust;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import joust.joustcache.ChecksumRunner;
import joust.optimisers.runnables.AssertionStrip;
import joust.optimisers.runnables.CSE;
import joust.optimisers.runnables.ConstFold;
import joust.optimisers.runnables.FinalFolder;
import joust.optimisers.runnables.LoopInvar;
import joust.optimisers.runnables.Unroll;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.conversion.TreeConverter;
import joust.utils.commandline.OptimiserOptions;
import joust.utils.compiler.JavacBrutaliser;
import joust.utils.compiler.OptimisationPhaseManager;
import joust.utils.compiler.StaticCompilerUtils;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import static joust.utils.compiler.OptimisationPhaseManager.PhaseModifier.*;
import static joust.utils.compiler.OptimisationPhaseManager.VirtualPhase.*;
import static com.sun.source.util.TaskEvent.Kind.*;
import static joust.utils.compiler.StaticCompilerUtils.javaElements;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("*")
@SupportedOptions({"JOUSTLogLevel", "JOUSTStripAssertions", "JOUSTMinCSEScore", "JOUSTHelp"})
public class JOUST extends AbstractProcessor {
    // The untranslated input JCTrees. The route to the AST prior to the desugaring step.
    public static LinkedHashSet<JCTree.JCCompilationUnit> conventionalTrees;

    public static JavacProcessingEnvironment environ;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        StaticCompilerUtils.uninit();
        AJCForest.uninit();

        environ = (JavacProcessingEnvironment) env;

        // Parse command line options.
        if (!OptimiserOptions.configureFromProcessingEnvironment(env)) {
            log.fatal("JOUST aborted by command line argument processor.");
            return;
        }

        // So we can log those fatal errors...
        LogUtils.init(processingEnv);

        conventionalTrees = new LinkedHashSet<>();

        OptimisationPhaseManager.init(env);
        OptimisationPhaseManager.register(new AssertionStrip(), AFTER, ANNOTATION_PROCESSING);
        OptimisationPhaseManager.register(new JavacBrutaliser(), AFTER, ANNOTATION_PROCESSING);

        // As it happens, almost all our phases operate on the virtual AFTER DESUGAR phase (as this turns out to be
        // very much more convenient than working on the actual tree if you don't care about the desugared things.)
        OptimisationPhaseManager.register(new TreeConverter(), AFTER, DESUGAR);
        OptimisationPhaseManager.register(new FinalFolder(), AFTER, DESUGAR);
        OptimisationPhaseManager.register(new ConstFold(), AFTER, DESUGAR);

        // TODO: Repair and re-enable this.
        // OptimisationPhaseManager.register(new AssignmentStrip(), AFTER, DESUGAR);
        OptimisationPhaseManager.register(new LoopInvar(), AFTER, DESUGAR);
        OptimisationPhaseManager.register(new Unroll(), AFTER, DESUGAR);
        OptimisationPhaseManager.register(new CSE(), AFTER, DESUGAR);

        // The post-compilation pass to populate the disk cache with the results of classes processed
        // during this job. Needs to happen here so we can compute a checksum over the bytecode and
        // spot when things get sneakily changed when we weren't looking.
        OptimisationPhaseManager.register(new ChecksumRunner(), AFTER, GENERATE);

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
