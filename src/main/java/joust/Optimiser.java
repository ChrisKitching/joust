package joust;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import joust.translators.AssertionStrippingTranslator;
import joust.translators.ConstFoldTranslator;
import joust.utils.LogUtils;
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
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public @Log4j2 class Optimiser extends AbstractProcessor {
    public static Trees mTrees;

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

        Set<? extends Element> elements = roundEnvironment.getRootElements();
        for (Element each : elements) {
            log.debug("Element: {}", each);
            if (each.getKind() == ElementKind.CLASS) {
                // Another magic cast to a compiler-internal type to get us the power we need.
                // The JCTree type gives us access to the entire AST, rather than just the methods.
                JCTree tree = (JCTree) mTrees.getTree(each);

                // Strip assertions, if set. Operation neglects possibility of side effects on
                // assertions. This defaults to disabled.
                if (OptimiserOptions.stripAssertions) {
                    AssertionStrippingTranslator stripper = new AssertionStrippingTranslator();
                    tree.accept(stripper);
                }

                ConstFoldTranslator constFold;
                do {
                    constFold = new ConstFoldTranslator();
                    tree.accept(constFold);
                } while (constFold.makingChanges());
            }
        }

        return false;
    }
}
