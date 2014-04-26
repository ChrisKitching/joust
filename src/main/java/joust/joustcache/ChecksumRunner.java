package joust.joustcache;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.joustcache.data.TransientClassInfo;
import joust.optimisers.runnables.OptimisationRunnable;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import javax.tools.JavaFileObject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
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
        log.info("ChecksumRunner started.");

        long s = System.currentTimeMillis();

        log.info("Keys: {}", Arrays.toString(JOUSTCache.classInfo.keySet().toArray()));
        Iterator<String> keyIterator = JOUSTCache.classInfo.keySet().iterator();
        while (keyIterator.hasNext()) {
            String className = keyIterator.next();
            TransientClassInfo transientInfo = JOUSTCache.transientClassInfo.get(className);
            if (transientInfo.flushed) {
                log.fatal("Already flushed: {}", className);
                continue;
            }

            try {
                // Get a reference to the file to which this ClassSymbol was written.
                JavaFileObject outFile =
                        fileManager.getJavaFileForOutput(CLASS_OUTPUT,
                        className,
                        JavaFileObject.Kind.CLASS,
                        transientInfo.getSourceFile());

                // Due to the operation of the BY_TODO compile policy, this runner gets called many
                // times. Not always are all results available.
                if (outFile == null) {
                    log.warn("Cannot flush {} because no outfile available!", className);
                    continue;
                }

                final int hash = ChecksumUtils.computeHash(outFile);

                JOUSTCache.writeSymbolToDisk(className, hash);
                transientInfo.setFlushed(true);
                log.info("Flush: {}", className);
            } catch (FileNotFoundException e) {
                // Probably just called too soon...
            } catch (IOException e) {
                log.error("Can't find file for: {}", className, e);
            }

            keyIterator.remove();
        }
        long e = System.currentTimeMillis();
        log.info("Done in " + (e - s) + "ms");
    }
}
