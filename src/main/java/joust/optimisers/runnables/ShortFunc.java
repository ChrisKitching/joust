package joust.optimisers.runnables;

import joust.optimisers.shortfunc.ShortFuncFunctionTemplates;
import joust.optimisers.shortfunc.ShortFuncTranslator;
import joust.optimisers.translators.ConstFoldTranslator;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class ShortFunc extends OptimisationRunnable.OneTwo {
    public ShortFunc() {
        super(new ShortFuncTranslator(), new ConstFoldTranslator());
    }

    @Override
    public void run() {
        ShortFuncFunctionTemplates.init();
        log.info("Short func running.");
        super.run();
    }
}
