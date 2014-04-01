package tests.unittests;

import com.sun.tools.javac.code.Symbol;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.utils.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import tests.unittests.utils.VisitorResultPurger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class BaseAnalyserTest<T extends AJCTreeVisitor> extends TreeFabricatingTest {
    // The class of the tree translator type of interest.
    private AJCTreeVisitor visitorInstance;

    private final String resultField;

    private final VisitorResultPurger purger = new VisitorResultPurger();

    /**
     * Construct a new BaseAnalyserTest for a given visitor class and result field.
     * The result field is the name of the field on the translated trees that holds the result of this analysis
     * step.
     */
    public BaseAnalyserTest(Class<T> translatorClass, String rField) {
        resultField = rField;

        try {
            visitorInstance = translatorClass.getConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("Error instantiating visitor.", e);
        }
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
            } catch (NoSuchFieldException | IllegalAccessException e) {
                log.error("Unable to find result field!", e);
                assertTrue(false);
            }

            log.debug("Checking live at {}", resultNode);
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
     * Shorthand function for creating sets of symbols for test parameters.
     */
    protected static Symbol.VarSymbol[] $v(Symbol.VarSymbol... vSyms) {
        return vSyms;
    }
}

