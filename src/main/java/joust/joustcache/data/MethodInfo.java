package joust.joustcache.data;

import com.sun.tools.javac.code.Symbol;
import joust.treeinfo.EffectSet;
import lombok.Data;

/**
 * Data class to hold the information we want to associate with each method in the disk cache. This
 * is generally the subset of the results of the analysis performed which is of interest outside the
 * compilation unit containing the method.
 */
public @Data
class MethodInfo {
    private final String methodHash;
    private final EffectSet effectSet;

    public static String getHashForMethod(Symbol.MethodSymbol sym) {
        return sym.owner.type.toString() + '.' + sym.name.toString() + ':' + sym.type.toString();
    }
}
