package joust.tree.annotatedtree;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.Optimiser;
import joust.joustcache.data.ClassInfo;
import joust.optimisers.visitors.sideeffects.SideEffectVisitor;
import joust.tree.conversion.TreePreparationTranslator;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.AJCTree.*;

/**
 * A forest of annotated Java ASTs. Typically, the input to the optimiser will be converted into a single such
 * forest.
 * Serves as a handy place for structures shared among all input trees to reside.
 */
@Log4j2
public class AJCForest {
    // The input root nodes, after conversion.
    public final List<AJCClassDecl> rootNodes;

    // Maps method symbols to their corresponding declaration nodes.
    public final HashMap<MethodSymbol, AJCMethodDecl> methodTable;

    // Used to provide a deserialisation target for VarSymbols.
    public final HashMap<String, VarSymbol> varsymbolTable;

    /**
     * Init the forest from the given set of root elements.
     * Iterate the root elements converting them to our tree representation, populating the various tables
     * as you go.
     */
    public static void init(Iterable<JCCompilationUnit> rootElements) {
        List<AJCClassDecl> prospectiveRootNodes = List.nil();

        // Since it's stateless...
        final TreePreparationTranslator sanity = new TreePreparationTranslator();

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

                    prospectiveRootNodes = prospectiveRootNodes.prepend(translatedTree);
                }
            }
        }

        // Deallocate the annoying lookup table.
        InitialASTConverter.uninit();

        AJCForest ret = new AJCForest(prospectiveRootNodes, prospectiveMethodTable, prospectiveVarSymbolTable);
        Optimiser.inputTrees = ret;
        ret.initialAnalysis();
    }

    /**
     * Perform initial analysis on the converted tree that has to occur prior to other optimisation steps taking place.
     */
    private void initialAnalysis() {
        log.info("Commencing initial side effect analysis on converted tree.");
        // Run the initial effect analysis on the tree (It's kept incrementally updated)...
        SideEffectVisitor effects = new SideEffectVisitor();
        for (AJCClassDecl tree : rootNodes) {
            effects.visit(tree);
        }

        // So now we've populated the direct effects of each method. Let's resolve all the loose ends...
        effects.bootstrap();
        log.info("Initial side effect analysis complete.");
    }

    // Prevent direct instantiation.
    private AJCForest(List<AJCClassDecl> trees, HashMap<MethodSymbol, AJCMethodDecl> mTable, HashMap<String, VarSymbol> vTable) {
        rootNodes = trees;
        methodTable = mTable;
        varsymbolTable = vTable;
    }
}
