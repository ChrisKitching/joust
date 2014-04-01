package tests.unittests;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.joustcache.JOUSTCache;
import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.treeinfo.TreeInfoManager;
import joust.utils.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * Base class for unit tests to test a type of TreeTranslators.
 *
 * @param <T> The type of TreeTranslator under test.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class BaseTreeTranslatorTest<T extends BaseTranslator> extends TreeFabricatingTest {
    // The class of the tree translator type of interest.
    private BaseTranslator translatorInstance;

    public BaseTreeTranslatorTest(Class<T> translatorClass) {
        try {
            translatorInstance = translatorClass.getConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("Error instantiating translator.", e);
        }
    }

    /**
     * Mock out the AJCForest instance.
     */
    private void prepareTrees(AJCTree input, AJCTree expected) {
        AJCForest.uninit();
        TreeInfoManager.init();

        HashMap<MethodSymbol, AJCMethodDecl> methodTable = new HashMap<>();
        HashMap<String, VarSymbol> vTable = new HashMap<>();
        if (input instanceof AJCMethodDecl) {
            AJCMethodDecl cast = (AJCMethodDecl) input;
            methodTable.put(cast.getTargetSymbol(), cast);
        }

        AJCForest.initDirect(List.of(input), methodTable, vTable);

        AJCForest.getInstance().effectVisitor.visitTree(expected);
    }

    /**
     * General method for running a translator function test. Takes the name of the tree translator
     * function to test, the input tree and the expected output tree, and runs the test.
     *
     * @param input The tree to pass to the translator function under test.
     * @param expected The expected result of applying this translator function to the input tree.
     */
    protected void testVisitNode(AJCTree input, AJCTree expected) {
        prepareTrees(input, expected);

        doVisit(input, false);

        log.info("Result: {}  Expected: {}", input, expected);

        // Ensure that the actual output matches the expected output.
        // TODO: It may become necessary to implement a proper comparator for JCTree objects.
        assertEquals(expected.toString(), input.toString());
    }

    protected void testVisitNodeBluntForce(AJCTree input, AJCTree expected) {
        prepareTrees(input, expected);

        doVisit(input, true);
        log.info("Result: {}  Expected: {}", input, expected);

        // Ensure that the actual output matches the expected output.
        // TODO: It may become necessary to implement a proper comparator for JCTree objects.
        assertEquals(expected.toString(), input.toString());
    }

    private void doVisit(AJCTree input, boolean bluntForce) {
        do {
            translatorInstance.visitTree(input);
        } while (bluntForce && translatorInstance.makingChanges());
    }
}
