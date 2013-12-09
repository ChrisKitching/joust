package joust.joustcache;

import com.sun.tools.javac.code.Symbol;
import joust.Optimiser;
import joust.optimisers.utils.OptimisationRunnable;
import joust.utils.LogUtils;
import lombok.extern.log4j.Log4j2;
import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;

import javax.tools.JavaFileObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

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

        List<Symbol.ClassSymbol> classSymbols = Optimiser.elementSymbols;
        for (Symbol.ClassSymbol c : classSymbols) {
            try {
                // Get a reference to the file to which this ClassSymbol was written.
                JavaFileObject outFile
                        = Optimiser.fileManager.getJavaFileForOutput(CLASS_OUTPUT,
                        c.flatname.toString(),
                        JavaFileObject.Kind.CLASS,
                        c.sourcefile);


                final int hash = ChecksumUtils.computeHash(outFile);

                log.debug("Hash for {} is {}", c, hash);

                JOUSTCache.writeSymbolToDisk(c, hash);
            } catch (IOException e) {
                LogUtils.raiseCompilerError("Error opening generated classfile to store analysis results! Your next incremental build will probably fail (Although this one probably didn't)");
            }
        }
        long e = System.currentTimeMillis();
        log.debug("Total time for ChecksumRunner is " + (e - s) + "ms");
    }
}
