package joust.tree.annotatedtree;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.analysers.sideeffects.Effects;
import joust.joustcache.JOUSTCache;
import joust.optimisers.normalise.TreeNormalisingTranslator;
import joust.analysers.sideeffects.SideEffectVisitor;
import joust.tree.conversion.TreePreparationTranslator;
import joust.tree.annotatedtree.treeinfo.TreeInfoManager;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.AJCTree.*;

/**
 * A forest of annotated Java ASTs. Typically, the input to the optimiser will be converted into a single such
 * forest.
 * Serves as a handy place for structures shared among all input trees to reside.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class AJCForest {
    private boolean analysisPerformed;

    private static AJCForest instance;
    // TODO: Something something context.
    public static AJCForest getInstance() {
        return instance;
    }

    // The input root nodes, after conversion.
    public final List<AJCTree> rootNodes;

    // Maps method symbols to their corresponding declaration nodes.
    public final HashMap<MethodSymbol, AJCMethodDecl> methodTable;

    public SideEffectVisitor effectVisitor = new SideEffectVisitor(analysisPerformed);

    /**
     * Init the forest from the given set of root elements.
     * Iterate the root elements converting them to our tree representation, populating the various tables
     * as you go.
     */
    public static void init(Iterable<JCCompilationUnit> rootElements) {
        if (instance != null) {
            throw new UnsupportedOperationException("Attempt to reassign AJCForest!");
        }

        long t = System.currentTimeMillis();

        List<AJCTree> prospectiveRootNodes = List.nil();

        // Since it's stateless...
        final TreePreparationTranslator sanity = new TreePreparationTranslator();
        final TreeNormalisingTranslator normaliser = new TreeNormalisingTranslator();

        HashMap<MethodSymbol, AJCMethodDecl> prospectiveMethodTable = new HashMap<>();

        InitialASTConverter.init();
        for (JCCompilationUnit element : rootElements) {
            // Perform the sanity translations on the tree that are more convenient to do before the translation step...
            element.accept(sanity);

            // Find the class definitions in here...
            for (JCTree def : element.defs) {
                if (def instanceof JCClassDecl) {
                    log.debug("Commence translation of: {}", def);

                    JCClassDecl classTree = (JCClassDecl) def;

                    // Translate the tree to our tree representation...
                    InitialASTConverter converter = new InitialASTConverter();
                    classTree.accept(converter);

                    AJCClassDecl translatedTree = (AJCClassDecl) converter.getResult();
                    log.debug("Translated tree: {}", translatedTree);

                    // Populate method and varsym tables.
                    for (AJCMethodDecl defN : translatedTree.methods) {
                        prospectiveMethodTable.put(defN.getTargetSymbol(), defN);
                        log.debug("Method: {}", defN.getTargetSymbol());
                    }

                    // Normalise the tree.
                    normaliser.visitTree(translatedTree);

                    prospectiveRootNodes = prospectiveRootNodes.prepend(translatedTree);
                }
            }
        }

        log.info("Tree converted and normalised in {}ms", System.currentTimeMillis() - t);

        // Deallocate the annoying lookup table.
        InitialASTConverter.uninit();
        initDirect(prospectiveRootNodes, prospectiveMethodTable);
    }

    public static void initDirect(List<AJCTree> trees, HashMap<MethodSymbol, AJCMethodDecl> mTable) {
        if (instance != null) {
            throw new UnsupportedOperationException("Attempt to reassign AJCForest!");
        }

        // Effect handler utils...
        TreeInfoManager.init();
        JOUSTCache.init();

        AJCForest ret = new AJCForest(trees, mTable);

        instance = ret;

        ret.initialAnalysis();
    }

    /**
     * *sigh*
     */
    public static void uninit() {
        log.warning("Uniniting AJCTree.");
        instance = null;
    }

    /**
     * Perform initial analysis on the converted tree that has to occur prior to other optimisation steps taking place.
     */
    public void initialAnalysis() {
        long t = System.currentTimeMillis();
        log.info("Initial side effect analysis started on {} nodes", rootNodes.size());

        effectVisitor = new SideEffectVisitor(!analysisPerformed);
        Effects.numCalls = 0;

        VisitorResultPurger purger = new VisitorResultPurger();

        log.info("Purging...");
        // Run the initial effect analysis on the tree (It's kept incrementally updated)...
        for (AJCTree tree : rootNodes) {
            purger.visitTree(tree);
        }

        log.info("Effect...");
        for (AJCTree tree : rootNodes) {
            effectVisitor.visitTree(tree);
        }

        // So now we've populated the direct effects of each method. Let's resolve all the loose ends...
        effectVisitor.bootstrap();

        analysisPerformed = true;
        log.info("numCalls: {}", Effects.numCalls);

        log.info("Initial side effect analysis completed in {}ms.", System.currentTimeMillis() - t);
    }

    // Prevent direct instantiation.
    private AJCForest(List<AJCTree> trees, HashMap<MethodSymbol, AJCMethodDecl> mTable) {
        rootNodes = trees;
        methodTable = mTable;
    }
}
