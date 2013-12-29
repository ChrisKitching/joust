package joust;

import static joust.optimisers.utils.OptimisationPhaseManager.PhaseModifier.*;
import static com.sun.source.util.TaskEvent.Kind.*;
import static com.sun.tools.javac.tree.JCTree.*;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import joust.joustcache.ChecksumRunner;
import joust.joustcache.ChecksumUtils;
import joust.joustcache.JOUSTCache;
import joust.joustcache.data.MethodInfo;
import joust.optimisers.ConstFold;
import joust.optimisers.ExpressionNormaliser;
import joust.optimisers.SideEffects;
import joust.optimisers.StripAssertions;
import joust.treeinfo.EffectSet;
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
import javax.tools.JavaFileManager;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public @Log4j2 class Optimiser extends AbstractProcessor {
    public static Trees mTrees;
    public static LinkedList<JCTree> elementTrees = new LinkedList<>();
    public static LinkedList<Symbol.ClassSymbol> elementSymbols = new LinkedList<>();

    // TODO: Consider moving.
    // Maps method symbol hashes to their corresponding declaration. Useful for loading summary
    // information from the disk cache.
    public static HashMap<String, JCTree.JCMethodDecl> methodTable = new HashMap<>();

    // Factory class, internal to the compiler, used to manufacture parse tree nodes.
    public static TreeMaker treeMaker;

    public static JavaFileManager fileManager;

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

        JOUSTCache.init();
        ChecksumUtils.init();
        EffectSet.init();

        // Define when we run each optimisation.
        OptimisationPhaseManager.init(env);
        OptimisationPhaseManager.register(new StripAssertions(), AFTER, ANNOTATION_PROCESSING);
        OptimisationPhaseManager.register(new ConstFold(), AFTER, ANNOTATION_PROCESSING);
        OptimisationPhaseManager.register(new SideEffects(), AFTER, ANALYZE);
        OptimisationPhaseManager.register(new ExpressionNormaliser(), AFTER, ANALYZE);

        // The post-compilation pass to populate the disk cache with the results of classes processed
        // during this job. Needs to happen here so we can compute a checksum over the bytecode and
        // spot when things get sneakily changed when we weren't looking.
        OptimisationPhaseManager.register(new ChecksumRunner(), AFTER, GENERATE);


        mTrees = Trees.instance(env);

        // We typecast the processing environment to the one used by javac. This dirty trick allows
        // us to alter the AST - something not generally possible in annotation processors.
        Context context = ((JavacProcessingEnvironment) env).getContext();

        treeMaker = TreeMaker.instance(context);
        fileManager = context.get(JavaFileManager.class);
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver()) {
            return false;
        }

        // Collect references to all the trees we're interested in. The actual optimisation occurs
        // in the various PhaseSpecificRunnables registered for execution later (Directly after this
        // step or otherwise).
        Set<? extends Element> elements = roundEnvironment.getRootElements();
        for (Element each : elements) {
            log.debug("Element: {}", each);
            if (each.getKind() == ElementKind.CLASS) {
                // Another magic cast to a compiler-internal type to get us the power we need.
                // The JCTree type gives us access to the entire AST, rather than just the methods.
                JCTree.JCClassDecl classTree = (JCTree.JCClassDecl) mTrees.getTree(each);

                elementSymbols.add(classTree.sym);
                elementTrees.add(classTree);

                // Populate method table.
                for (JCTree def : classTree.defs) {
                    if (def instanceof JCMethodDecl) {
                        JCMethodDecl cast = (JCMethodDecl) def;

                        methodTable.put(MethodInfo.getHashForMethod(cast.sym), cast);
                    }
                }
            }
        }

        return false;
    }
}
