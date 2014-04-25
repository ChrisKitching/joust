package joust.joustcache.data;

import com.sun.tools.javac.code.Symbol;
import joust.analysers.sideeffects.Effects;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Data class to hold the information we want to associate with each method in the disk cache. This
 * is generally the subset of the results of the analysis performed which is of interest outside the
 * compilation unit containing the method.
 */
@NoArgsConstructor
@AllArgsConstructor
public class MethodInfo {
    public String methodHash;
    public Effects effectSet;

    public static String getHashForMethod(Symbol.MethodSymbol sym) {
        return sym.owner.type.toString() + '.' + sym.name.toString() + ':' + sym.type.toString();
    }

    @Override
    public String toString() {
        return methodHash + ':' + effectSet;
    }
}
