package tests.unittests;

import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Base class for unit tests to test a type of TreeTranslators.
 *
 * @param <T> The type of TreeTranslator under test.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class BaseTreeTranslatorTest<T extends BaseTranslator> extends BaseTreeVisitorTest<T> {
    public BaseTreeTranslatorTest(Class<T> translatorClass, Object... constructorArgs) {
        super(translatorClass, constructorArgs);
    }

    /**
     * General method for running a translator function test. Takes the name of the tree translator
     * function to test, the input tree and the expected output tree, and runs the test.
     *
     * @param input The tree to pass to the translator function under test.
     * @param expected The expected result of applying this translator function to the input tree.
     */
    private void testVisitNode(AJCTree input, AJCTree expected, boolean bluntForce) {

        prepareTrees(input, expected);

        if (bluntForce) {
            doVisitBluntForce(input);
        } else {
            doVisit(input);
        }

        log.info("Result: {}  Expected: {}", input, expected);

        // Ensure that the actual output matches the expected output.
        // TODO: It may become necessary to implement a proper comparator for JCTree objects.
        assertEquals(expected.toString(), input.toString());
    }
    protected void testVisitNode(AJCTree input, AJCTree expected) {
        testVisitNode(input, expected, false);
    }
    protected void testVisitNodeBluntForce(AJCTree input, AJCTree expected) {
        testVisitNode(input, expected, true);
    }

    /**
     * Repeatedly apply the visitor instance until it reports no more changes to the tree.
     * @param input Tree to apply.
     */
    private void doVisitBluntForce(AJCTree input) {
        do {
            visitorInstance.visitTree(input);
        } while (visitorInstance.makingChanges());
    }
}
