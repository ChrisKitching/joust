package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

/**
 * Base class for optimising translators. Exposes the result value to the unit tester.
 */
public class BaseTranslator extends TreeTranslator {
    public JCTree getResult() {
        return result;
    }
}
