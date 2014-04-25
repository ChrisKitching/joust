package joust.joustcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import joust.joustcache.data.ClassInfo;
import joust.tree.annotatedtree.AJCForest;
import joust.utils.logging.LogUtils;
import joust.utils.data.SymbolSet;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;

/**
 * Serialiser for SymbolSets. Adds support for the universal set singleton, and ensures elements are serialised using
 * the VarSymbolSerialiser to ensure they get reconsituted correctly.
 * Symbols present at serialisation time may not be meaningfully representable in the deserialisation context. As such,
 * the object you get back may be radicially different from the one you put in, but it is certain to only contain
 * that subset of the symbols originally stored that you actually care about.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class SymbolSetSerialiser extends Serializer<SymbolSet> {
    @Override
    public void write(Kryo kryo, Output output, SymbolSet object) {
        log.info("Writing: {}", object);
        // Record if this is the universal set or not.
        if (object == SymbolSet.UNIVERSAL_SET) {
            output.writeByte(1);
            return;
        }

        output.writeByte(0);
        output.writeInt(object.size());

        for (VarSymbol sym : object) {
            // TODO: xxHash might improve size?
            output.writeAscii(ClassInfo.getHashForVariable(sym));
        }
    }

    @Override
    public SymbolSet read(Kryo kryo, Input input, Class<SymbolSet> type) {
        byte isUniversalSet = input.readByte();
        if (isUniversalSet == (byte) 1) {
            return SymbolSet.UNIVERSAL_SET;
        }

        SymbolSet ret = new SymbolSet();
        int len = input.readInt();

        for (int i = 0; i < len; i++) {
            String symbolHash = input.readString();
            log.info("Got symbol hash: {}", symbolHash);

            // Determine if this is a symbol we care about...
            if (JOUSTCache.varSymbolTable.containsKey(symbolHash)) {
                VarSymbol symGot = JOUSTCache.varSymbolTable.get(symbolHash);
                log.info("Obtained concrete symbol: {}", symGot);
                ret.add(symGot);
            } else {
                log.info("Discarded.");
            }
        }

        return ret;
    }
}
