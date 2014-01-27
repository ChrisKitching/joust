package joust.treeinfo;

import static com.sun.tools.javac.tree.JCTree.*;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import joust.joustcache.JOUSTCache;
import joust.joustcache.data.ClassInfo;
import joust.joustcache.data.MethodInfo;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableExpression;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.sun.tools.javac.code.Symbol.*;

/**
 * Provides access to TreeInfo objects related to tree nodes.
 */
public @Log4j2
class TreeInfoManager {
    private static HashMap<JCTree, TreeInfo> mTreeInfoMap;

    // Maps method symbol hashes to the effect sets of their corresponding JCMethodDecl nodes, which
    // may or may not actually *exist* in the parsed code.
    private static HashMap<String, TreeInfo> mMethodInfoMap;

    private static HashMap<MethodSymbol, Set<VarSymbol>> mEverLives = new HashMap<>();

    public static void init() {
        mTreeInfoMap = new HashMap<>();
        mMethodInfoMap = new HashMap<>();
    }

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
        if (tree instanceof JCMethodDecl) {
            JCMethodDecl castTree = (JCMethodDecl) tree;

            mMethodInfoMap.put(MethodInfo.getHashForMethod(castTree.sym), infoNode);
            //JOUSTCache.registerMethodSideEffects(((JCTree.JCMethodDecl) tree).sym, effects);
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
     * Get the EffectSet for the provided method symbol, or the set of all effects if no EffectSet
     * can be found.
     *
     * @param sym MethodSymbol to seek effects for.
     * @return The corresponding EffectSet from either the memory or disk cache, or the set of all
     *         effects if no such EffectSet exists.
     */
    public static EffectSet getEffectsForMethod(Symbol.MethodSymbol sym) {
        TreeInfo infoNode = mMethodInfoMap.get(MethodInfo.getHashForMethod(sym));
        if (infoNode != null) {
            return infoNode.mEffectSet;
        }

        // Attempt to find the missing info in the cache.
        // JOUSTCache.loadCachedInfoForClass((Symbol.ClassSymbol) sym.owner);

        infoNode = mMethodInfoMap.get(MethodInfo.getHashForMethod(sym));
        if (infoNode != null) {
            return infoNode.mEffectSet;
        }

        log.warn("Unable to source side effects for method: {}. This will harm optimisation - such calls are taken to have all possible side effects!", sym);
        return EffectSet.ALL_EFFECTS;
    }

    public static HashSet<PotentiallyAvailableExpression> getAvailable(JCTree tree) {
        TreeInfo infoNode = getInfoNode(tree);
        return infoNode.potentiallyAvailable;
    }

    /**
     * Populate the info structures from the given ClassInfo object. ClassInfo objects store class
     * and method granularity information that can meaningfully be cached between compilation units.
     * Assumes the hash checking has already taken place.
     *
     * @param cInfo The ClassInfo object to parse.
     */
    public static void populateFromClassInfo(ClassInfo cInfo) {
        for (MethodInfo mI : cInfo.methodInfos) {
            TreeInfo infoNode = new TreeInfo();
            infoNode.mEffectSet = mI.getEffectSet();
            mMethodInfoMap.put(mI.getMethodHash(), infoNode);
        }
    }

    public static void registerAvailables(JCTree tree, HashSet<PotentiallyAvailableExpression> avail) {
        TreeInfo infoNode = getInfoNode(tree);
        infoNode.potentiallyAvailable = avail;
    }

    public static void registerLives(JCTree tree, Set<Symbol> live) {
        TreeInfo infoNode = getInfoNode(tree);
        infoNode.liveVariables = live;
    }

    public static Set<Symbol> getLiveVariables(JCTree tree) {
        TreeInfo infoNode = getInfoNode(tree);
        return infoNode.liveVariables;
    }

    /**
     * Register the ever-live set for a given method.
     */
    public static void setEverLiveForMethod(JCMethodDecl jcMethodDecl, HashSet<VarSymbol> everLive) {
        mEverLives.put(jcMethodDecl.sym, everLive);
    }


    public static Set<VarSymbol> getEverLive(MethodSymbol meth) {
        return mEverLives.get(meth);
    }
}
