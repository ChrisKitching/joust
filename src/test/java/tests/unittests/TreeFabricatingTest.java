package tests.unittests;

import com.sun.tools.javac.util.Context;
import joust.utils.StaticCompilerUtils;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;


/**
 * Base class for unit tests that require the ability to fabricate AST fragments as test inputs.
 */
@Log4j2
public abstract class TreeFabricatingTest {
    @BeforeClass
    public static void init() {
        if (StaticCompilerUtils.isInitialised()) {
            return;
        }

        log.debug("TreeFabricatingTest init.");
        Context context = new Context();
        StaticCompilerUtils.initWithContext(context);
    }

    public TreeFabricatingTest() {
        init();
    }
}
