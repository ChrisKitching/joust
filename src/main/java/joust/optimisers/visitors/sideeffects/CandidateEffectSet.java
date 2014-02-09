package joust.optimisers.visitors.sideeffects;

import com.sun.tools.javac.code.Symbol;
import joust.treeinfo.EffectSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CandidateEffectSet {
    public static final CandidateEffectSet NO_EFFECTS = new CandidateEffectSet(); {
        {
            concreteEffects = EffectSet.NO_EFFECTS;
            needsEffectsFrom = Collections.emptySet();
        }
    }

    EffectSet concreteEffects;
    Symbol.MethodSymbol targetMethod;
    Set<Symbol.MethodSymbol> needsEffectsFrom = new HashSet<>();

    public static CandidateEffectSet wrap(EffectSet concrete) {
        CandidateEffectSet ret = new CandidateEffectSet();
        ret.concreteEffects = concrete;
        return ret;
    }

    /**
     * Union this candidate effect set with another, returning a new candidat effect set.
     */
    public CandidateEffectSet union(CandidateEffectSet other, boolean escaping) {
        CandidateEffectSet ret = new CandidateEffectSet();
        if (escaping) {
            ret.concreteEffects = concreteEffects.unionEscaping(other.concreteEffects);
        } else {
            ret.concreteEffects = concreteEffects.union(other.concreteEffects);
        }
        ret.needsEffectsFrom = new HashSet<>(needsEffectsFrom);
        ret.needsEffectsFrom.addAll(other.needsEffectsFrom);
        return ret;
    }
    public CandidateEffectSet union(CandidateEffectSet other) {
        return union(other, false);
    }

    public CandidateEffectSet union(CandidateEffectSet... others) {
        CandidateEffectSet ret = new CandidateEffectSet();
        ret.concreteEffects = concreteEffects;
        for (CandidateEffectSet other : others) {
            ret.concreteEffects = ret.concreteEffects.union(other.concreteEffects);
            ret.needsEffectsFrom.addAll(other.needsEffectsFrom);
        }

        return ret;
    }

    public CandidateEffectSet union(EffectSet effectSet) {
        CandidateEffectSet ret = union(NO_EFFECTS);
        ret.concreteEffects = concreteEffects.union(effectSet);

        return ret;
    }

    @Override
    public String toString() {
        String prefix = "CES{Concrete: " + concreteEffects + "\nNeeds: " + Arrays.toString(needsEffectsFrom.toArray());
        if (targetMethod != null) {
            return prefix + "\nFor: " + targetMethod + "}\n";
        }
        return prefix + "}\n";
    }

    /**
     * Return a new CES that also requires the effects from the given MethodSymbol
     */
    public CandidateEffectSet alsoRequires(Symbol.MethodSymbol methodSym) {
        // Create a copy...
        CandidateEffectSet ret = union(NO_EFFECTS);
        ret.needsEffectsFrom.add(methodSym);

        return ret;
    }
}
