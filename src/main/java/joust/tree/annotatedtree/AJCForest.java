package joust.tree.annotatedtree;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.List;
import joust.Optimiser;
import joust.joustcache.JOUSTCache;
import joust.joustcache.data.ClassInfo;
import joust.optimisers.translators.ExpressionNormalisingTranslator;
import joust.optimisers.visitors.sideeffects.SideEffectVisitor;
import joust.tree.conversion.TreePreparationTranslator;
import joust.treeinfo.TreeInfoManager;
import joust.utils.LogUtils;
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
    private static AJCForest instance;
    // TODO: Something something context.
    public static AJCForest getInstance() {
        return instance;
    }

    // The input root nodes, after conversion.
    public final List<AJCTree> rootNodes;

    // Maps method symbols to their corresponding declaration nodes.
    public final HashMap<MethodSymbol, AJCMethodDecl> methodTable;

    // Used to provide a deserialisation target for VarSymbols.
    public final HashMap<String, VarSymbol> varsymbolTable;

    public SideEffectVisitor effectVisitor;

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
        final ExpressionNormalisingTranslator normaliser = new ExpressionNormalisingTranslator();

        HashMap<MethodSymbol, AJCMethodDecl> prospectiveMethodTable = new HashMap<>();
        HashMap<String, VarSymbol> prospectiveVarSymbolTable = new HashMap<>();

        InitialASTConverter.init();
        for (JCCompilationUnit element : rootElements) {
            // Find the class definitions in here...
            for (JCTree def : element.defs) {
                if (def instanceof JCClassDecl) {
                    log.debug("Commence translation of: {}", def);

                    JCClassDecl classTree = (JCClassDecl) def;

                    // Perform the sanity translations on the tree that are more convenient to do before the translation step...
                    classTree.accept(sanity);

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

                    for (AJCVariableDecl defN : translatedTree.fields) {
                        prospectiveVarSymbolTable.put(ClassInfo.getHashForVariable(defN.getTargetSymbol()), defN.getTargetSymbol());
                        log.debug("Field: {}", defN.getTargetSymbol());
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
        initDirect(prospectiveRootNodes, prospectiveMethodTable, prospectiveVarSymbolTable);
    }

    public static void initDirect(List<AJCTree> trees, HashMap<MethodSymbol, AJCMethodDecl> mTable, HashMap<String, VarSymbol> vTable) {
        if (instance != null) {
            throw new UnsupportedOperationException("Attempt to reassign AJCForest!");
        }


        // Effect handler utils...
        TreeInfoManager.init();
        JOUSTCache.init();

        AJCForest ret = new AJCForest(trees, mTable, vTable);

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
        log.info("Initial side effect analysis started.");
        effectVisitor = new SideEffectVisitor();

        // Run the initial effect analysis on the tree (It's kept incrementally updated)...
        for (AJCTree tree : rootNodes) {
            effectVisitor.visit(tree);
        }

        // So now we've populated the direct effects of each method. Let's resolve all the loose ends...
        effectVisitor.bootstrap();
        log.info("Initial side effect analysis completed in {}ms.", System.currentTimeMillis() - t);
    }

    // Prevent direct instantiation.
    private AJCForest(List<AJCTree> trees, HashMap<MethodSymbol, AJCMethodDecl> mTable, HashMap<String, VarSymbol> vTable) {
        rootNodes = trees;
        methodTable = mTable;
        varsymbolTable = vTable;
    }
}
