package joust.joustcache;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

import joust.Optimiser;
import joust.joustcache.data.TransientClassInfo;
import joust.optimisers.utils.OptimisationRunnable;
import lombok.extern.log4j.Log4j2;
import javax.tools.JavaFileObject;
import java.io.IOException;

/**
 * A phase to be run after writing the classfiles which computes the checksums of the produced files
 * and writes them and the relavent analysis results to the disk cache.
 */
public @Log4j2
class ChecksumRunner implements OptimisationRunnable {

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
                        = Optimiser.fileManager.getJavaFileForOutput(CLASS_OUTPUT,
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
