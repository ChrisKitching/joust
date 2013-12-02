package joust;

import static joust.optimisers.utils.OptimisationPhaseManager.PhaseModifier.*;
import static com.sun.source.util.TaskEvent.Kind.*;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import joust.optimisers.ConstFold;
import joust.optimisers.StripAssertions;
import joust.utils.LogUtils;
import joust.optimisers.utils.OptimisationPhaseManager;
import lombok.extern.log4j.Log4j2;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.LinkedList;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public @Log4j2 class Optimiser extends AbstractProcessor {
    public static Trees mTrees;
    public static LinkedList<JCTree> elementTrees = new LinkedList<>();

    // Factory class, internal to the compiler, used to manufacture parse tree nodes.
    public static TreeMaker treeMaker;

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

        // Define when we run each optimisation.
        OptimisationPhaseManager.init(env);
        OptimisationPhaseManager.register(new StripAssertions(), AFTER, ANNOTATION_PROCESSING);
        OptimisationPhaseManager.register(new ConstFold(), AFTER, ANNOTATION_PROCESSING);

        mTrees = Trees.instance(env);

        // We typecast the processing environment to the one used by javac. This dirty trick allows
        // us to alter the AST - something not generally possible in annotation processors.
        Context context = ((JavacProcessingEnvironment) env).getContext();

        treeMaker = TreeMaker.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver()) {
            log.info("Optimiser has concluded.");
            return false;
        }

        log.info("Optimiser starting.");

        // Collect references to all the trees we're interested in. The actual optimisation occurs
        // in the various PhaseSpecificRunnables registered for execution later (Directly after this
        // step or otherwise).
        Set<? extends Element> elements = roundEnvironment.getRootElements();
        for (Element each : elements) {
            log.debug("Element: {}", each);
            if (each.getKind() == ElementKind.CLASS) {
                // Another magic cast to a compiler-internal type to get us the power we need.
                // The JCTree type gives us access to the entire AST, rather than just the methods.
                elementTrees.add((JCTree) mTrees.getTree(each));
            }
        }

        return false;
    }
}
