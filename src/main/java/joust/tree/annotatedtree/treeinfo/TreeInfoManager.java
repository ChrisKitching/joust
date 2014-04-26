package joust.tree.annotatedtree.treeinfo;

import joust.joustcache.JOUSTCache;
import joust.joustcache.data.ClassInfo;
import joust.joustcache.data.MethodInfo;
import joust.analysers.sideeffects.Effects;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;

/**
 * Provides access to TreeInfo objects related to tree nodes.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public final class TreeInfoManager {
    // Maps method symbol hashes to the effect sets of their corresponding JCMethodDecl nodes, which
    // may or may not actually *exist* in the parsed code.
    private static HashMap<String, Effects> methodEffectMap;

    public static void init() {
        methodEffectMap = new HashMap<String, Effects>();
    }

    /**
     * Add an EffectSet to the method effect table...
     */
    public static void registerMethodEffects(MethodSymbol sym, Effects effects, boolean shouldSave) {
        methodEffectMap.put(MethodInfo.getHashForMethod(sym), effects);

        // Don't save the set of all effects. No point.
        if (effects.getEffectSet().contains(EffectSet.ALL_EFFECTS)) {
            log.info("Declining to save all-effects set for {}", sym);
            return;
        }

        if (shouldSave) {
            JOUSTCache.registerMethodSideEffects(sym, effects);
        }
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

        log.debug("Unable to source side effects for method: {}. This will harm optimisation - such calls are taken to have all possible side effects!", sym);
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
}
