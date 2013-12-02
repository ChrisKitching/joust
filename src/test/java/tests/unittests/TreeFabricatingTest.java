package tests.unittests;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import joust.Optimiser;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;

import javax.tools.JavaFileManager;

/**
 * Base class for unit tests that require the ability to fabricate AST fragments as test inputs.
 */
public @Log4j2
abstract class TreeFabricatingTest {
    // Factory class, internal to the compiler, used to manufacture parse tree nodes.
    protected static TreeMaker t;

    @BeforeClass
    public static void init() {
        if (t == null) {
            log.debug("Init time!");
            Context context = new Context();
            JavacFileManager fileManager = new JavacFileManager(context, true, null);
            context.put(JavaFileManager.class, fileManager);
            t = TreeMaker.instance(context);

            // Make the TreeMaker instance available to unit tests, as well as the test harness.
            Optimiser.treeMaker = t;
        }
    }

    public TreeFabricatingTest() {
        init();
    }
}
