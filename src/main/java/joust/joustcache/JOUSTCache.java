package joust.joustcache;

import static com.sun.tools.javac.code.Symbol.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import joust.analysers.sideeffects.Effects;
import joust.joustcache.data.ClassInfo;
import joust.joustcache.data.MethodInfo;
import joust.joustcache.data.TransientClassInfo;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import joust.tree.annotatedtree.treeinfo.TreeInfoManager;
import joust.utils.logging.LogUtils;
import joust.utils.data.SymbolSet;
import lombok.Cleanup;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * The on-disk cache of analysis results.
 */
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@Log
public class JOUSTCache {
    private static final String DATABASE_FILE_NAME = "joustCache";
    private static final int INITIAL_BUFFER_SIZE = 100000;

    // For serialising ClassInfo objects before putting them in the database.
    private static final Kryo serialiser = new Kryo();

    // The PrimaryTreeMap backed by the database. Note that each call to get will cause partial
    // deserialisation. Key is class name, value is the serialised version of the ClassInfo object,
    // which includes the hash of the compiled class file - to detect changes.
    private static PrimaryTreeMap<String, byte[]> databaseMap;
    private static String databasePath;
    private static RecordManager databaseRecordManager;

    static HashMap<String, ClassInfo> classInfo = new HashMap<>();
    static HashMap<String, TransientClassInfo> transientClassInfo = new HashMap<>();

    // Used to provide a deserialisation target for Symbols.
    public static final HashMap<String, VarSymbol> varSymbolTable = new HashMap<>();
    public static final HashMap<String, MethodSymbol> methodSymbolTable = new HashMap<>();

    public static void init() {
        log.info("Init JOUSTCache!");
        varSymbolTable.clear();
        methodSymbolTable.clear();
        transientClassInfo.clear();
        classInfo.clear();

        ChecksumUtils.init();
        if (databaseRecordManager != null) {
            return;
        }
        try {
            databaseRecordManager = getOrCreateRecordManager();
        } catch (IOException e) {
            log.fatal("Unable to create or open local data cache.\n" + e);
            return;
        }

        databaseMap = databaseRecordManager.treeMap(databasePath);

        initSerialiser();
    }

    /**
     * Construct an appropriately-tuned Kryo object to serialise the ClassInfo and MethodInfo objects.
     */
    private static void initSerialiser() {
        // Register first, so the default serialiser doesn't override the custom one later on...
        serialiser.register(LinkedList.class);
        serialiser.register(String.class);

        // Register the classes with the serialiser to enable slightly more concise output.
        serialiser.setRegistrationRequired(true);

        // Let's live dangerously.
        serialiser.setAsmEnabled(false);

        // Serialiser for ClassInfo
        FieldSerializer classInfoSerialiser = new FieldSerializer(serialiser, ClassInfo.class);
        classInfoSerialiser.setFieldsCanBeNull(false);

        // Serialiser for MethodInfo
        FieldSerializer methodInfoSerialiser = new FieldSerializer(serialiser, MethodInfo.class);
        methodInfoSerialiser.getField("methodHash").setClass(String.class, new DefaultSerializers.StringSerializer());
        methodInfoSerialiser.setFieldsCanBeNull(false);

        // Serialiser for EffectSet.
        FieldSerializer effectSetSerialiser = new FieldSerializer(serialiser, EffectSet.class);

        SymbolSetSerialiser symbolSetSerialiser = new SymbolSetSerialiser();
        serialiser.register(SymbolSet.class, symbolSetSerialiser);

        // Don't serialise non-escaping symbol sets. Nobody cares.
        effectSetSerialiser.removeField("readInternal");
        effectSetSerialiser.removeField("writeInternal");
        effectSetSerialiser.getField("readEscaping").setClass(SymbolSet.class, symbolSetSerialiser);
        effectSetSerialiser.getField("writeEscaping").setClass(SymbolSet.class, symbolSetSerialiser);

        serialiser.register(EffectSet.class, effectSetSerialiser);

        // Now you can serialise an EffectSet, you can serialise an Effects.
        serialiser.register(Effects.class, new EffectsSerialiser());

        methodInfoSerialiser.getField("effectSet").setClass(EffectSet.class, new EffectsSerialiser());
        serialiser.register(MethodInfo.class, methodInfoSerialiser);

        // To serialise the list of MethodInfo objects inside the ClassInfo.
        CollectionSerializer methodInfoListSerialiser = new CollectionSerializer();
        methodInfoListSerialiser.setElementsCanBeNull(false);
        methodInfoListSerialiser.setElementClass(MethodInfo.class, methodInfoSerialiser);

        // Register the list serialiser with the class serialiser.
        classInfoSerialiser.getField("methodInfos").setClass(LinkedList.class, methodInfoListSerialiser);
        serialiser.register(ClassInfo.class, classInfoSerialiser);
    }

    /**
     * Helper method to get the RecordManager, possibly creating the parent directory at the same time
     * (If this is the first time it has been used).
     *
     * @return A jdbm2 RecordManager for the local data cache.
     * @throws IOException If RecordManagerFactory.createRecordManager throws such an exception.
     */
    private static RecordManager getOrCreateRecordManager() throws IOException {
        String homeDirectory = System.getProperty("user.home");
        File joustDir = new File(homeDirectory + "/.joust/");

        if (!joustDir.exists()) {
            if (!joustDir.mkdir()) {
                log.fatal("Unable to create directory: " + joustDir + " for local data cache.");
                return null;
            }
            log.info("Created directory {}", joustDir);
        }

        databasePath = homeDirectory + "/.joust/" + DATABASE_FILE_NAME;

        log.info("Creating or opening database at {}", databasePath);
        return RecordManagerFactory.createRecordManager(databasePath);
    }

    /**
     * Load the cached analysis results for the given ClassSymbol
     *
     * @param sym ClassSymbol from which to load definitions.
     */
    public static void loadCachedInfoForClass(ClassSymbol sym) {
        if (sym == null) {
            return;
        }

        byte[] payload = databaseMap.get(sym.fullname.toString());
        if (payload == null) {
            log.debug("No cached info for class {} seems to exist.", sym.fullname.toString());
            return;
        }

        if (sym.classfile == null) {
            log.warn("Unable to load cached info for class {}. No classfile given. Bug?", sym.fullname.toString());
            return;
        }

        log.info("Loaded {} bytes of cached info for class {}", payload.length, sym.fullname.toString());

        @Cleanup UnsafeInput deserialiserInput = new UnsafeInput(payload);
        ClassInfo cInfo = serialiser.readObject(deserialiserInput, ClassInfo.class);

        log.info("Loaded info:\n{}", cInfo);

        if (cInfo == null) {
            log.warn("Unable to load cached info - got null - for class {}", sym.fullname.toString());
            return;
        }

        // Check hashes, discard the result if the hash doesn't match up.
        int classHash;
        try {
            classHash = ChecksumUtils.computeHash(sym.classfile);
        } catch (IOException e) {
            log.warn("Unable to load cached info for class {}.\nIOException computing hash: {}", sym.fullname.toString(), e);
            return;
        }

        if (classHash != cInfo.hash) {
            log.warn("Hash mismatch for: {}\n" +
                    "Classfile hash: {}\n" +
                    "Classinfo hash: {}\n", sym.fullname.toString(), classHash, cInfo.hash);
            return;
        }

        TreeInfoManager.populateFromClassInfo(cInfo);
    }

    static void writeSymbolToDisk(String className, int hash) {
        final ClassInfo cInfo = classInfo.get(className);

        if (cInfo == null) {
            log.warn("Unexpectedly null ClassInfo for {} at cache write.", className);
            return;
        }

        cInfo.setHash(hash);

        log.warn("Serialising {} for {}", cInfo, className);

        // Serialise the ClassInfo object.
        @Cleanup UnsafeOutput serialisedOutput = new UnsafeOutput(INITIAL_BUFFER_SIZE);
        serialiser.writeObject(serialisedOutput, cInfo);

        byte[] buffer = serialisedOutput.toBytes();

        log.debug("Serialised using {} bytes", buffer.length);

        databaseMap.put(className, buffer);
        try {
            databaseRecordManager.commit();
        } catch (IOException e) {
            log.fatal("IOException flushing to disk cache:", e);
        }
    }

    /**
     * Register the given EffectSet with the given MethodSymbol. Things so registered will be
     * written to the cache when we exit.
     *
     * @param sym Method symbol to relate the side effects with.
     * @param effectSet The effect set of the provided method symbol's declaration.
     */
    public static void registerMethodSideEffects(MethodSymbol sym, Effects effectSet) {
        final String methodHash = MethodInfo.getHashForMethod(sym);
        final String className = ((ClassSymbol) sym.owner).flatname.toString();

        MethodInfo m = new MethodInfo(methodHash, effectSet);
        log.debug("{} has effects {} in {}", methodHash, effectSet, className);

        ClassInfo cInfo = classInfo.get(className);
        if (cInfo == null) {
            cInfo = new ClassInfo();
            classInfo.put(className, cInfo);
        }
        cInfo.methodInfos.add(m);

        // So we can compute the checksums later on...
        TransientClassInfo tcInfo = transientClassInfo.get(className);
        if (tcInfo == null) {
            tcInfo = new TransientClassInfo();
            transientClassInfo.put(className, tcInfo);
        }

        tcInfo.setSourceFile(((ClassSymbol) sym.owner).sourcefile);
    }
}
