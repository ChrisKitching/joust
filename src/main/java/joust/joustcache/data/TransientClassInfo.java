package joust.joustcache.data;

import lombok.Data;

import javax.tools.JavaFileObject;

/**
 * Data class used to store class info that should not be persisted between runs.
 */
@Data
public class TransientClassInfo {
    public JavaFileObject sourceFile;

    // Has the corresponding ClassInfo object been flushed to disk this session?
    public boolean flushed = false;
}
