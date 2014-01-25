package joust.joustcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import joust.treeinfo.EffectSet;

/**
 * A Kryo serialiser for EffectSets.
 * TODO: Support serialising the affected symbol sets!
 */
public class EffectSetSerialiser  extends Serializer<EffectSet> {
    @Override
    public void write (Kryo kryo, Output output, EffectSet object) {
        output.writeInt(object.effectTypes);
    }

    @Override
    public EffectSet read (Kryo kryo, Input input, Class<EffectSet> type) {
        return new EffectSet(input.readInt());
    }
}
