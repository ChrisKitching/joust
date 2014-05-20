package joust;

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Pair;
import joust.joustcache.ChecksumRunner;
import joust.joustcache.JOUSTCache;
import joust.optimisers.runnables.AssertionStrip;
import joust.optimisers.runnables.CSE;
import joust.optimisers.runnables.CleanupRunner;
import joust.optimisers.runnables.ConstFold;
import joust.optimisers.runnables.FinalFolder;
import joust.optimisers.runnables.LoopInvar;
import joust.optimisers.runnables.ShortFunc;
import joust.optimisers.runnables.Unbox;
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
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;
import static joust.utils.compiler.OptimisationPhaseManager.EventType.*;
import static joust.utils.compiler.StaticCompilerUtils.javaElements;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("*")
@SupportedOptions({"JOUSTLogLevel", "JOUSTStripAssertions", "JOUSTMinCSEScore", "JOUSTHelp", "JOUSTAnnotateLib", "JOUSTPrintEffectCacheKeys"})
public class JOUST extends AbstractProcessor {
    // The untranslated input JCTrees. The route to the AST prior to the desugaring step.
    public static Queue<Pair<Env<AttrContext>, JCClassDecl>> environmentsToProcess;

    public static JavacProcessingEnvironment environ;
    public static HashSet<JCCompilationUnit> conventionalTrees;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        StaticCompilerUtils.uninit();
        AJCForest.uninit();

        environ = (JavacProcessingEnvironment) env;
        conventionalTrees = new HashSet<JCCompilationUnit>();

        // So we can log those fatal errors...
        LogUtils.init(processingEnv);

        // Parse command line options.
        if (!OptimiserOptions.configureFromProcessingEnvironment(env)) {
            log.fatal("JOUST aborted by command line argument processor.");
            return;
        }


        if (OptimiserOptions.dumpingEffectKeys) {
            JOUSTCache.init();
            JOUSTCache.dumpKeys();
            JOUSTCache.closeDatabase();
            return;
        }

        OptimisationPhaseManager.init();
        if (OptimiserOptions.stripAssertions) {
            OptimisationPhaseManager.register(new AssertionStrip(), AFTER_ANNOTATION_PROCESSING);
        }

        OptimisationPhaseManager.register(new JavacBrutaliser(), AFTER_ANNOTATION_PROCESSING);
        OptimisationPhaseManager.register(new TreeConverter(), AFTER_DESUGAR);

        // As it happens, almost all our phases operate in the AFTER_DESUGAR phase (as this turns out to be
        // very much more convenient than working on the actual tree if you don't care about the desugared things.)

        // Don't optimise in library annotation mode - we just need one pass of the SideEffectVisitor (via TreeConverter)
        // and we're done,
        if (!OptimiserOptions.annotatingLibrary) {
            OptimisationPhaseManager.register(new FinalFolder(), AFTER_DESUGAR);
            OptimisationPhaseManager.register(new ConstFold(), AFTER_DESUGAR);
            OptimisationPhaseManager.register(new ShortFunc(), AFTER_DESUGAR);
            OptimisationPhaseManager.register(new Unbox(), AFTER_DESUGAR);

            // TODO: Repair and re-enable this.
            // OptimisationPhaseManager.register(new AssignmentStrip(), AFTER, DESUGAR);
            OptimisationPhaseManager.register(new LoopInvar(), AFTER_DESUGAR);
            OptimisationPhaseManager.register(new Unroll(), AFTER_DESUGAR);
            OptimisationPhaseManager.register(new CSE(), AFTER_DESUGAR);
        }

        // The post-compilation pass to populate the disk cache with the results of classes processed
        // during this job. Needs to happen here so we can compute a checksum over the bytecode and
        // spot when things get sneakily changed when we weren't looking.
        OptimisationPhaseManager.register(new ChecksumRunner(), AFTER_GENERATE);

        // Close any resources that need closing...
        OptimisationPhaseManager.register(new CleanupRunner(), AFTER_GENERATE);

        StaticCompilerUtils.init(env);
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        // Nothing to process if dumping effect keys.
        if (OptimiserOptions.dumpingEffectKeys) {
            return false;
        }

        if (!roundEnvironment.processingOver()) {
            return false;
        }

        for (Element element : roundEnvironment.getRootElements()) {
            // Stash the root elements for later use...
            JCCompilationUnit unit = javaElements.getTreeAndTopLevel(element, null, null).snd;

            conventionalTrees.add(unit);
        }

        // Launch the brutaliser!
        OptimisationPhaseManager.dispatchEvent(AFTER_ANNOTATION_PROCESSING);

        // Stop other annotation processors from causing problems by running later.
        return true;
    }
}
