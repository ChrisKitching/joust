package tests.unittests;

import com.sun.tools.javac.util.Context;
import joust.utils.LogUtils;
import joust.utils.StaticCompilerUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.BeforeClass;

import java.util.logging.Logger;


/**
 * Base class for unit tests that require the ability to fabricate AST fragments as test inputs.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class TreeFabricatingTest {
    @BeforeClass
    public static void init() {
        if (StaticCompilerUtils.isInitialised()) {
            return;
        }

        log.debug("TreeFabricatingTest init.");
        Context context = new Context();
        StaticCompilerUtils.initWithContext(context);
        LogUtils.init(null);
    }

    public TreeFabricatingTest() {
        init();
    }
}
