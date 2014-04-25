package joust.joustcache;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.joustcache.data.TransientClassInfo;
import joust.optimisers.runnables.OptimisationRunnable;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import javax.tools.JavaFileObject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
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
        log.info("Event: {}", currentEvent);

        List<String> keysToProcess = List.nil();

        List<JCTree> trees = ((JCTree.JCCompilationUnit) currentEvent.getCompilationUnit()).defs;
        for (JCTree t : trees) {
            if (t instanceof JCTree.JCClassDecl) {
                JCTree.JCClassDecl cast = (JCTree.JCClassDecl) t;
                keysToProcess = keysToProcess.prepend(cast.sym.flatName().toString());
            }
        }

        long s = System.currentTimeMillis();

        for (String className : JOUSTCache.classInfo.keySet()) {
            TransientClassInfo transientInfo = JOUSTCache.transientClassInfo.get(className);
            if (transientInfo.flushed) {
                continue;
            }

            try {
                // Get a reference to the file to which this ClassSymbol was written.
                log.info("Looking for a file for {}", className);
                if (transientInfo != null) {
                    log.info("Transient: {}", transientInfo.getSourceFile());
                }

                JavaFileObject outFile =
                        fileManager.getJavaFileForOutput(CLASS_OUTPUT,
                        className,
                        JavaFileObject.Kind.CLASS,
                        transientInfo.getSourceFile());

                // Due to the operation of the BY_TODO compile policy, this runner gets called many
                // times. Not always are all results available.
                if (outFile == null) {
                    continue;
                }

                final int hash = ChecksumUtils.computeHash(outFile);
                log.info("Hash for {} is {}", className, hash);

                JOUSTCache.writeSymbolToDisk(className, hash);
                transientInfo.setFlushed(true);
            } catch (FileNotFoundException e) {
                // Probably just called too soon...
            } catch (IOException e) {
                log.error("Can't find file for: {}", className, e);
            }
        }
        long e = System.currentTimeMillis();
        log.info("Done in " + (e - s) + "ms");
    }
}
