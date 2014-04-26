package joust.analysers.sideeffects;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import joust.utils.logging.LogUtils;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Class to hold an EffectSet and the dependency information for it to support incremental updating.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class Effects {
    EffectSet directPart;

    @Getter
    EffectSet effectSet;

    // Effects depending on this one.
    public Set<Effects> dependantOnThis = new HashSet<>();

    // Effects on which this one depends.
    public Set<Effects> deps = new HashSet<>();


    public Effects(EffectSet computed, EffectSet direct) {
        effectSet = computed.union(direct);
        directPart = direct;
    }

    /**
     * Shift all the dependency relations from this Effects to the target.
     */
    public void reparent(Effects target) {
        target.deps = deps;
        target.dependantOnThis = dependantOnThis;

        for (Effects e : dependantOnThis) {
            e.deps.remove(this);
            e.deps.add(target);
        }

        for (Effects e : deps) {
            e.dependantOnThis.remove(this);
            e.dependantOnThis.add(target);
        }

        deps = null;
        dependantOnThis = null;
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

        EffectSet[] effectSets = new EffectSet[es.length];
        for (int i = 0; i < es.length; i++) {
            effectSets[i] = es[i].effectSet;
            es[i].dependantOnThis.add(newEffects);
            newEffects.deps.add(es[i]);
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

    public static int numCalls;

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
        numCalls++;
        if (visited.contains(this)) {
            return;
        }
        visited.add(this);
        log.trace("Rebuilding effects from children...");

        EffectSet newEffectSet = EffectSet.NO_EFFECTS.union(directPart);
        log.trace("Old: {}", newEffectSet);
        for (Effects child : deps) {
            if (child == this) {
                continue;
            }

            if (!visited.contains(child)) {
                child.rebuildFromChildrenInternal(visited);
            }

            newEffectSet = newEffectSet.union(child.effectSet);
        }
        log.trace("New: {}", newEffectSet);

        setEffectSetInternal(newEffectSet, visited);
    }

    @Override
    public String toString() {
        return "Immediate: " + directPart.toString()+"     Computed: " + effectSet.toString();
    }
}
