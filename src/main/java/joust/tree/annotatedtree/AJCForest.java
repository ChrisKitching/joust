package joust.tree.annotatedtree;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.joustcache.data.ClassInfo;
import joust.tree.conversion.TreePreparationTranslator;
import lombok.extern.log4j.Log4j2;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.HashMap;
import java.util.Set;

import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.StaticCompilerUtils.*;

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
    public static AJCForest init(Set<? extends Element> rootElements) {
        List<AJCClassDecl> prospectiveRootNodes = List.nil();

        // Since it's stateless...
        final TreePreparationTranslator sanity = new TreePreparationTranslator();

        HashMap<MethodSymbol, AJCMethodDecl> prospectiveMethodTable = new HashMap<>();
        HashMap<String, VarSymbol> prospectiveVarSymbolTable = new HashMap<>();

        InitialASTConverter.init();
        for (Element element : rootElements) {
            log.debug("Element: {}", element);
            if (element.getKind() != ElementKind.CLASS) {
                log.info("Ignoring element of type {}" + element.getKind());
                continue;
            }

            // A magic cast to the compiler-internal AST type gets us the read-write access we need...
            JCTree.JCClassDecl classTree = (JCTree.JCClassDecl) trees.getTree(element);

            // Perform the sanity translations on the tree that are more convenient to do before the translation step...
            classTree.accept(sanity);

            // Translate the tree to our tree represenation...
            InitialASTConverter converter = new InitialASTConverter();
            classTree.accept(converter);

            AJCClassDecl translatedTree = (AJCClassDecl) converter.getResult();

            // Populate method and varsym tables.
            for (AJCMethodDecl def : translatedTree.methods) {
                prospectiveMethodTable.put(def.getTargetSymbol(), def);
            }

            for (AJCVariableDecl def : translatedTree.fields) {
                prospectiveVarSymbolTable.put(ClassInfo.getHashForVariable(def.getTargetSymbol()), def.getTargetSymbol());
            }

            prospectiveRootNodes = prospectiveRootNodes.prepend(translatedTree);
        }

        // Deallocate the annoying lookup table.
        InitialASTConverter.uninit();

        return new AJCForest(prospectiveRootNodes, prospectiveMethodTable, prospectiveVarSymbolTable);
    }

    // Prevent direct instantiation.
    private AJCForest(List<AJCClassDecl> trees, HashMap<MethodSymbol, AJCMethodDecl> mTable, HashMap<String, VarSymbol> vTable) {
        rootNodes = trees;
        methodTable = mTable;
        varsymbolTable = vTable;
    }
}
