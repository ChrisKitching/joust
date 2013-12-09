package joust.joustcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import joust.treeinfo.EffectSet;

/**
 * A Kryo serialiser for EffectSets. Due to their singleton nature, EffectSets can be represented
 * entirely as their mask.
 */
public class EffectSetSerialiser  extends Serializer<EffectSet> {
    @Override
    public void write (Kryo kryo, Output output, EffectSet object) {
        output.writeInt(object.effectMask);
    }

    @Override
    public EffectSet read (Kryo kryo, Input input, Class<EffectSet> type) {
        EffectSet.init();
        return EffectSet.getEffectSet(input.readInt());
    }
}
