package joust;

import joust.joustcache.ChecksumUtils;
import joust.joustcache.JOUSTCache;
import joust.optimisers.ConstFold;
import joust.optimisers.ExpressionNormalise;
import joust.optimisers.avail.normalisedexpressions.PossibleSymbol;
import joust.optimisers.utils.OptimisationPhaseManager;
import joust.tree.annotatedtree.AJCForest;
import joust.treeinfo.TreeInfoManager;
import joust.utils.LogUtils;
import joust.utils.StaticCompilerUtils;
import lombok.extern.log4j.Log4j2;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static com.sun.source.util.TaskEvent.Kind.*;
import static joust.optimisers.utils.OptimisationPhaseManager.PhaseModifier.*;

@Log4j2
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("*")
public class Optimiser extends AbstractProcessor {
    public static AJCForest inputTrees;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        // So we can log those fatal errors...
        LogUtils.init(processingEnv);

        // Parse command line options.
        if (!OptimiserOptions.configureFromProcessingEnvironment(env)) {
            LogUtils.raiseCompilerError("Optimiser aborted by command line argument processor.");
            return;
        }

        inputTrees = null;

        JOUSTCache.init();
        ChecksumUtils.init();
        PossibleSymbol.init();
        TreeInfoManager.init();

        // Define when we run each optimisation.
        OptimisationPhaseManager.init(env);
        OptimisationPhaseManager.register(new ConstFold(), AFTER, ANNOTATION_PROCESSING);
        OptimisationPhaseManager.register(new ExpressionNormalise(), AFTER, ANALYZE);
        //OptimisationPhaseManager.register(new SideEffects(), AFTER, ANALYZE);
        //OptimisationPhaseManager.register(new AssignmentStrip(), AFTER, ANALYZE);
        //OptimisationPhaseManager.register(new LoopInvar(), AFTER, ANALYZE);
        //OptimisationPhaseManager.register(new Unroll(), AFTER, ANALYZE);

        // The post-compilation pass to populate the disk cache with the results of classes processed
        // during this job. Needs to happen here so we can compute a checksum over the bytecode and
        // spot when things get sneakily changed when we weren't looking.
        //OptimisationPhaseManager.register(new ChecksumRunner(), AFTER, GENERATE);


        StaticCompilerUtils.init(env);
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver()) {
            return false;
        }

        // Construct the representation of the AST that we're going to be working with...
        inputTrees = AJCForest.init(roundEnvironment.getRootElements());

        return false;
    }
}
