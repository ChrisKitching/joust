package joust;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class Optimiser extends AbstractProcessor {
    public static Trees mTrees;

    // Factory class, internal to the compiler, used to manufacture parse tree nodes.
    public static TreeMaker treeMaker;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        mTrees = Trees.instance(env);

        // We typecast the processing environment to the one used by javac. This dirty trick allows
        // us to alter the AST - something not generally possible in annotation processors.
        Context context = ((JavacProcessingEnvironment) env).getContext();

        treeMaker = TreeMaker.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Optimiser has concluded.");
            return false;
        }

        Set<? extends Element> elements = roundEnvironment.getRootElements();
        for (Element each : elements) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Element:"+each);
            if (each.getKind() == ElementKind.CLASS) {
                // Another magic cast to a compiler-internal type to get us the power we need.
                // The JCTree type gives us access to the entire AST, rather than just the methods.
                JCTree tree = (JCTree) mTrees.getTree(each);
                TreeTranslator visitor = new IfStatementTrueifier();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Visiting.");
                tree.accept(visitor);
            }
        }

        return false;
    }
}
