package tests.unittests;

import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.utils.logging.LogUtils;
import joust.utils.ReflectionUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import tests.unittests.utils.VisitorResultPurger;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class BaseAnalyserTest<T extends AJCTreeVisitor> extends BaseTreeVisitorTest<T> {
    private final String resultField;

    // Purges results from the tree.
    private final VisitorResultPurger purger = new VisitorResultPurger();

    /**
     * Construct a new BaseAnalyserTest for a given visitor class and result field.
     * The result field is the name of the field on the translated trees that holds the result of this analysis
     * step.
     */
    public BaseAnalyserTest(Class<T> translatorClass, String rField, Object... constructorArgs) {
        super(translatorClass, constructorArgs);
        resultField = rField;
    }

    /**
     * General method for running a visitor function test. Takes the tree to visit, the node of the tree
     * from which the result field should be read, and the expected value of the result field after the
     * visitor has been run.
     *
     * @param input The tree to pass to the visitor under test.
     * @param resultNodes The tree to read the result field from.
     * @param resultValue The expected value of the result field on the result node.
     */
    protected void testVisitNode(AJCTree input, Object[] resultNodes, Object[] resultValue) {
        log.debug("LVA on {}", input);
        visitorInstance.visitTree(input);

        for (int i = 0; i < resultNodes.length; i++) {
            Object resultNode = resultNodes[i];

            Class<?> resultClass = resultNode.getClass();
            Object resultObject = null;
            try {
                Field rField = resultClass.getField(resultField);
                rField.setAccessible(true);
                resultObject = rField.get(resultNode);
            } catch (NoSuchFieldException e) {
                log.error("Unable to find result field!", e);
                assertTrue(false);
            } catch (IllegalAccessException e) {
                log.error("Unable to find result field!", e);
                assertTrue(false);
            }

            log.debug("Checking {} at {}", resultField, resultNode);
            Level logLevel = Level.INFO;
            if (!resultValue[i].equals(resultObject)) {
                logLevel = Level.SEVERE;
            }

            log.log(logLevel, "Expecting: " + resultValue[i]);
            log.log(logLevel, "Found:     " + resultObject);

            assertTrue(resultValue[i].equals(resultObject));
        }

        purger.visit(input);
    }

    /**
     * Test function for stateful visitors - ones for which the analysis results are a property of the visitor itself,
     * instead of being stored on the tree nodes.
     */
    protected void testVisitNodeStatefully(AJCTree input, Object resultValue, Object... constructorArgs) {
        // Get a fresh visitor instance for the next test.
        createVisitorInstance(constructorArgs);

        visitorInstance.visitTree(input);

        Object actualResult;
        try {
            Field rField = ReflectionUtils.findField(visitorClass, resultField);
            rField.setAccessible(true);
            actualResult = rField.get(visitorInstance);
        } catch (NoSuchFieldException e) {
            log.error("Error during test!", e);
            fail();
            return;
        } catch (IllegalAccessException e) {
            log.error("Error during test!", e);
            fail();
            return;
        }

        boolean testResult = actualResult.equals(resultValue);
        if (testResult) {
            log.debug("Expected: {}", resultValue);
            log.debug("Actual:   {}", resultValue);
        } else {
            log.error("Expected: {}", resultValue);
            log.error("Actual:   {}", resultValue);
        }

        assertTrue(testResult);
    }
}

