package joust.joustcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import joust.analysers.sideeffects.Effects;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import joust.tree.annotatedtree.treeinfo.TreeInfoManager;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.logging.Logger;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class EffectsSerialiser extends Serializer<Effects> {
    private static final Field directPartF;
    private static final Field computedPartF;
//    private static final Field backwardDepsF;
//    private static final Field depsF;

    static {
        final Class<Effects> eClass = Effects.class;

        Field directPart = null;
        Field computedPart = null;
//        Field backwardDeps = null;
//        Field deps = null;

        try {
            directPart = eClass.getDeclaredField("directPart");
            computedPart = eClass.getDeclaredField("effectSet");
//            backwardDeps = eClass.getDeclaredField("dependantOnThis");
//            deps = eClass.getDeclaredField("deps");

            directPart.setAccessible(true);
            computedPart.setAccessible(true);
//            backwardDeps.setAccessible(true);
//            deps.setAccessible(true);
        } catch (NoSuchFieldException e) {
            log.fatal("Reflection error serialising Effects!", e);
        }

        directPartF = directPart;
        computedPartF = computedPart;
//        backwardDepsF = backwardDeps;
//        depsF = deps;
    }

    @Override
    public void write(Kryo kryo, Output output, Effects effects) {
        EffectSet directPart;
        EffectSet computedPart;
        Set<Effects> backwardDeps;
        Set<Effects> deps;
        try {
            directPart = (EffectSet) directPartF.get(effects);
            computedPart = (EffectSet) computedPartF.get(effects);
//            backwardDeps = (Set<Effects>) backwardDepsF.get(effects);
//            deps = (Set<Effects>) depsF.get(effects);
        } catch (IllegalAccessException e) {
            log.fatal("Reflection error serialising Effects!", e);
            return;
        }

        kryo.writeObject(output, directPart);
        kryo.writeObject(output, computedPart);

        // We're just going to tactfully omit dependency information for now. That shit's complicated.
    }

    @Override
    public Effects read(Kryo kryo, Input input, Class<Effects> effectsClass) {
        EffectSet directPart = kryo.readObject(input, EffectSet.class);
        EffectSet computedPart = kryo.readObject(input, EffectSet.class);

        Effects ret = new Effects(computedPart, directPart);

        return ret;
    }
}
