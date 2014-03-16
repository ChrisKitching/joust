package joust.optimisers.visitors.sideeffects;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.tree.annotatedtree.AJCTree;
import joust.treeinfo.EffectSet;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * Class to hold an EffectSet and the dependency information for it to support incremental updating.
 */
public class Effects {
    EffectSet directPart;

    @Getter
    EffectSet effectSet;

    // Effects depending on this one.
    Set<Effects> dependantOnThis = new HashSet<>();

    // Effects on which this one depends.
    Set<Effects> deps = new HashSet<>();

    // Unresolved method dependencies. Null after bootstrapping phase...
    Set<Symbol.MethodSymbol> needsEffectsFrom;

    public Effects(EffectSet computed, EffectSet direct) {
        effectSet = computed.union(direct);
        directPart = direct;
    }

    public Effects(EffectSet eSet) {
        this(eSet, EffectSet.NO_EFFECTS);
    }

    /**
     * Create a new Effects that contains the union of the effects in the es, marking the new set as depending on each of
     * the constitutents.
     */
    public static Effects unionOf(Effects... es) {
        Effects newEffects = new Effects(EffectSet.NO_EFFECTS);

        Set<Symbol.MethodSymbol> unresolved = new HashSet<>();
        EffectSet[] effectSets = new EffectSet[es.length];
        for (int i = 0; i < es.length; i++) {
            effectSets[i] = es[i].effectSet;
            es[i].dependantOnThis.add(newEffects);
            newEffects.deps.add(es[i]);

            if (es[i].needsEffectsFrom != null) {
                unresolved.addAll(es[i].needsEffectsFrom);
            }
        }

        if (!unresolved.isEmpty()) {
            newEffects.needsEffectsFrom = unresolved;
        }

        newEffects.effectSet = EffectSet.NO_EFFECTS.union(effectSets);
        return newEffects;
    }

    public static Effects unionWithDirect(EffectSet direct, Effects... es) {
        Effects newEffects = unionOf(es);
        newEffects.directPart = direct;
        newEffects.effectSet = newEffects.effectSet.union(direct);
        return newEffects;
    }

    public static Effects unionTrees(AJCTree.AJCEffectAnnotatedTree... trees) {
        Effects[] effects = new Effects[trees.length];
        for (int i = 0; i < trees.length; i++) {
            effects[i] = trees[i].effects;
        }

        return unionOf(effects);
    }

    public static Effects unionTrees(List<? extends AJCTree.AJCEffectAnnotatedTree> trees) {
        int size = trees.size();
        Effects[] effects = new Effects[size];

        int i = 0;
        for (AJCTree.AJCEffectAnnotatedTree tree : trees) {
            effects[i] = tree.effects;
            i++;
        }

        return unionOf(effects);
    }

    /**
     * Set the effect set to the given EffectSet and, if necessary, update the dependent effect sets.
     * @param e
     */
    public void setEffectSet(EffectSet e) {
        setEffectSetInternal(e, new HashSet<Effects>());
    }
    private void setEffectSetInternal(EffectSet e, Set<Effects> visited) {
        if (visited.contains(this)) {
            return;
        }
        visited.add(this);

        EffectSet oldEffects = effectSet;
        effectSet = e;
        if (dependantOnThis.isEmpty()) {
            return;
        }

        if (oldEffects.equals(e)) {
            return;
        }

        // Determine if this change only *added* effects. If so we can use a simpler routine to update...
        if (e.contains(oldEffects)) {
            // Union your way up the tree...
            for (Effects eS : dependantOnThis) {
                if (eS == this) {
                    continue;
                }
                eS.setEffectSetInternal(e.union(eS.effectSet), visited);
            }

            return;
        }

        // Otherwise, we've removed some stuff so have to do it the hard way...
        for (Effects eS : dependantOnThis) {
            if (eS == this) {
                continue;
            }
            eS.rebuildFromChildrenInternal(visited);
        }
    }

    /**
     * Recalculate this effect set from the children and its direct part.
     */
    public void rebuildFromChildren() {
        rebuildFromChildrenInternal(new HashSet<Effects>());
    }
    private void rebuildFromChildrenInternal(Set<Effects> visited) {
        if (visited.contains(this)) {
            return;
        }
        visited.add(this);

        EffectSet newEffectSet = directPart;
        for (Effects child : deps) {
            if (child == this) {
                continue;
            }

            newEffectSet = newEffectSet.union(child.effectSet);
        }

        setEffectSetInternal(newEffectSet, visited);
    }

    public void addUnresolvedDependency(Symbol.MethodSymbol target) {
        if (needsEffectsFrom == null) {
            needsEffectsFrom = new HashSet<>();
        }

        needsEffectsFrom.add(target);
    }
}
