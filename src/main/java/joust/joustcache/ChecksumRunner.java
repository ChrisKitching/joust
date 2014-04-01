package joust.joustcache;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

import joust.joustcache.data.TransientClassInfo;
import joust.optimisers.runnables.OptimisationRunnable;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.logging.Logger;

import static joust.utils.compiler.StaticCompilerUtils.fileManager;

/**
 * A phase to be run after writing the classfiles which computes the checksums of the produced files
 * and writes them and the relavent analysis results to the disk cache.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class ChecksumRunner extends OptimisationRunnable {
    @Override
    public void run() {
        log.debug("ChecksumRunner starting!");
        long s = System.currentTimeMillis();

        for (String className : JOUSTCache.classInfo.keySet()) {
            TransientClassInfo transientInfo = JOUSTCache.transientClassInfo.get(className);
            if (transientInfo.flushed) {
                continue;
            }

            try {
                // Get a reference to the file to which this ClassSymbol was written.
                JavaFileObject outFile
                        = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
                        className,
                        JavaFileObject.Kind.CLASS,
                        transientInfo.getSourceFile());

                // Due to the operation of the BY_TODO compile policy, this runner gets called many
                // times. Not always are all results available.
                if (outFile == null) {
                    continue;
                }

                final int hash = ChecksumUtils.computeHash(outFile);
                log.debug("Hash for {} is {}", className, hash);

                JOUSTCache.writeSymbolToDisk(className, hash);
                transientInfo.setFlushed(true);
            } catch (IOException e) {
                log.debug("Can't find file for: {}", className);
            }
        }
        long e = System.currentTimeMillis();
        log.debug("Total time for ChecksumRunner is " + (e - s) + "ms");
    }
}
