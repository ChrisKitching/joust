package joust.treeinfo;

import static com.sun.tools.javac.tree.JCTree.*;

import joust.joustcache.JOUSTCache;
import joust.joustcache.data.ClassInfo;
import joust.joustcache.data.MethodInfo;
import joust.optimisers.visitors.sideeffects.Effects;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.sun.tools.javac.code.Symbol.*;

/**
 * Provides access to TreeInfo objects related to tree nodes.
 */
@Log4j2
public final class TreeInfoManager {
    // Maps method symbol hashes to the effect sets of their corresponding JCMethodDecl nodes, which
    // may or may not actually *exist* in the parsed code.
    private static HashMap<String, Effects> methodEffectMap;

    private static HashMap<MethodSymbol, Set<VarSymbol>> mEverLives;

    public static void init() {
        methodEffectMap = new HashMap<>();
        mEverLives = new HashMap<>();
    }

    /**
     * Add an EffectSet to the method effect table...
     */
    public static void registerMethodEffects(MethodSymbol sym, Effects effects) {
        methodEffectMap.put(MethodInfo.getHashForMethod(sym), effects);
    }

    /**
     * Get the EffectSet for the provided method symbol, or the set of all effects if no EffectSet
     * can be found.
     *
     * @param sym MethodSymbol to seek effects for.
     * @return The corresponding EffectSet memory, or the set of all effects if no such EffectSet is found.
     */
    public static Effects getEffectsForMethod(MethodSymbol sym) {
        String symbolHash = MethodInfo.getHashForMethod(sym);

        Effects effects = methodEffectMap.get(symbolHash);
        if (effects != null) {
            return effects;
        }

        log.warn("Unable to source side effects for method: {}. This will harm optimisation - such calls are taken to have all possible side effects!", sym);
        return new Effects(EffectSet.ALL_EFFECTS, EffectSet.ALL_EFFECTS);
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
            methodEffectMap.put(mI.methodHash, mI.effectSet);
        }
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
