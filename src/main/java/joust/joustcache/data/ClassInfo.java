package joust.joustcache.data;

import lombok.Data;

import java.util.LinkedList;

/**
 * The data we want to assoiciate with each class in the persistent data storage.
 */
public @Data
class ClassInfo {
    public final LinkedList<MethodInfo> methodInfos = new LinkedList<>();
    public int hash;
}
