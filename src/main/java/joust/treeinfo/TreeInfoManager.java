package joust.treeinfo;

import static com.sun.tools.javac.tree.JCTree.*;

import com.sun.tools.javac.tree.JCTree;
import joust.Optimiser;
import joust.joustcache.JOUSTCache;
import joust.joustcache.data.ClassInfo;
import joust.joustcache.data.MethodInfo;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;

/**
 * Provides access to TreeInfo objects related to tree nodes.
 */
public @Log4j2
class TreeInfoManager {
    private static HashMap<JCTree, TreeInfo> mTreeInfoMap = new HashMap<>();

    /**
     * Ensures the TreeInfo node for the given JCTree exists and returns it. If no node exists, a
     * new one is created, stored, and returned.
     *
     * @param tree The TreeInfo node for the given JCTree.
     */
    private static TreeInfo getInfoNode(JCTree tree) {
        TreeInfo ret = mTreeInfoMap.get(tree);
        if (ret == null) {
            ret = new TreeInfo();
            mTreeInfoMap.put(tree, ret);
        }

        return ret;
    }

    /**
     * Register the given EffectSet with the given JCTree
     */
    public static void registerEffects(JCTree tree, EffectSet effects) {
        TreeInfo infoNode = getInfoNode(tree);
        infoNode.mEffectSet = effects;

        // If the tree node is a method declaration, we'll want to cache the EffectSet on disk for
        // use in future incremental compilation tasks.
        if (tree instanceof JCTree.JCMethodDecl) {
            JOUSTCache.registerMethodSideEffects(((JCTree.JCMethodDecl) tree).sym, effects);
        }
    }

    /**
     * Get the effect set stored for a particular tree node.
     *
     * @param tree The node for which an effect set is desired.
     * @return The effect set of that node, or null if none has yet been calculated.
     */
    public static EffectSet getEffects(JCTree tree) {
        TreeInfo infoNode = getInfoNode(tree);
        return infoNode.mEffectSet;
    }

    /**
     * Populate the info structures from the given ClassInfo object. ClassInfo objects store class
     * and method granularity information that can meaningfully be cached between compilation units.
     * Assumes the hash checking has already taken place.
     * By using a one-way hash to associate MethodInfo objects with MethodSymbols, we will be unable
     * to map a MethodInfo to a MethodSymbol here if the associated method is not used in any of the
     * compilation units under consideration. This has the most convenient property of causing us to
     * be *unable* to redundantly load the MethodInfo relating to a method that is unused in the
     * optimisation target.
     *
     * @param cInfo The ClassInfo object to parse.
     */
    public static void populateFromClassInfo(ClassInfo cInfo) {
        for (MethodInfo mI : cInfo.methodInfos) {
            JCMethodDecl decl = Optimiser.methodTable.get(mI.getMethodHash());
            if (decl != null) {
                log.debug("Loaded EffectSet {} for method {}", mI.getEffectSet(), mI.getMethodHash());
                registerEffects(decl, mI.getEffectSet());
            }
        }
    }
}
