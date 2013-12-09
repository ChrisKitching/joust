package joust.joustcache;

import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;

public class ChecksumUtils {
    private static final int INITIAL_BUFFER_SIZE = 500000;
    private static final int HASH_SEED = 42;

    // The size, in bytes, of the smallest input for which use of JNI beats use of Unsafe.
    private static final int JNI_UNSAFE_THRESHOLD = 1024;

    // The two hasher instances we find useful. The JNI one executes more quickly than the unsafe
    // one, but has a large constant overhead to start. Benchmarking shows that the one based on
    // sun.misc.Unsafe is faster for files <= 1024 bytes long.
    private static XXHash32 jniHasher;
    private static XXHash32 unsafeHasher;

    public static void init() {
        if (jniHasher != null) {
            return;
        }
        // The instances of XXHash32 are thread safe, according to the documentation.
        jniHasher = XXHashFactory.nativeInstance().hash32();
        unsafeHasher = XXHashFactory.unsafeInstance().hash32();
    }

    /**
     * Compute and return the hash of the given class file.
     *
     * @param classFile JavaFileObject representing the class file to compute the checksum of.
     * @return The checksum of the input file.
     */
    public static int computeHash(JavaFileObject classFile) throws IOException {
        // We roll our own slightly tiresome read-fully logic to avoid needing to reallocate a buffer
        // for each file (An annoying property of the library implementations,it seems).
        int bufferSize = INITIAL_BUFFER_SIZE;

        // TODO: Consider sharing buffers between calls to save allocations.
        byte[] buffer = new byte[bufferSize];

        // A pointer to the empty space in the buffer with smallest index.
        int bPointer = 0;

        // The quantity of bytes the last call to read yielded.
        int lastRead = 0;

        InputStream inputStream = classFile.openInputStream();
        try {
            while (lastRead != -1) {
                // Read as many bytes as are currently available into the buffer.
                lastRead = inputStream.read(buffer, bPointer, buffer.length-bPointer);
                bPointer += lastRead;

                // If buffer has overflowed, double its size and carry on.
                if (bPointer == buffer.length) {
                    bufferSize *= 2;
                    byte[] newBuffer = new byte[bufferSize];

                    // Copy the contents of the old buffer into the new buffer.
                    System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                    buffer = newBuffer;
                }
            }
        } finally {
            inputStream.close();
        }

        // Having loaded the class file into memory, (and with bPointer pointing to the first
        // index in the buffer which is *not* part of the loaded data), we compute the checksum.
        int hash;
        if (bPointer > JNI_UNSAFE_THRESHOLD) {
            hash = jniHasher.hash(buffer, 0, bPointer, HASH_SEED);
        } else {
            hash = unsafeHasher.hash(buffer, 0, bPointer, HASH_SEED);
        }

        return hash;
    }
}
