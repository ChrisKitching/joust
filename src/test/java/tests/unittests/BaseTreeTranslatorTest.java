package tests.unittests;

import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCTree;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;

/**
 * Base class for unit tests to test a type of TreeTranslators.
 *
 * @param <T> The type of TreeTranslator under test.
 */
@Log4j2
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
     * General method for running a translator function test. Takes the name of the tree translator
     * function to test, the input tree and the expected output tree, and runs the test.
     *
     * @param input The tree to pass to the translator function under test.
     * @param expected The expected result of applying this translator function to the input tree.
     */
    protected void testVisitNode(AJCTree input, AJCTree expected) {
        translatorInstance.visit(input);

        log.info("Result: {}  Expected: {}", input, expected);

        // Ensure that the actual output matches the expected output.
        // TODO: It may become necessary to implement a proper comparator for JCTree objects.
        assertEquals(expected.toString(), input.toString());
    }
}
