package joust.joustcache;

import static com.sun.tools.javac.code.Symbol.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import joust.joustcache.data.ClassInfo;
import joust.joustcache.data.MethodInfo;
import joust.joustcache.data.TransientClassInfo;
import joust.treeinfo.EffectSet;
import joust.treeinfo.TreeInfoManager;
import joust.utils.LogUtils;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * The on-disk cache of analysis results.
 */
public @Log4j2
class JOUSTCache {
    private static final String DATABASE_FILE_NAME = "joustCache";
    private static final int INITIAL_BUFFER_SIZE = 100000;

    private static HashSet<ClassSymbol> loadedSymbols = new HashSet<>();

    // For serialising ClassInfo objects before putting them in the database.
    private static Kryo serialiser = new Kryo();

    // The PrimaryTreeMap backed by the database. Note that each call to get will cause partial
    // deserialisation. Key is class name, value is the serialised version of the ClassInfo object,
    // which includes the hash of the compiled class file - to detect changes.
    private static PrimaryTreeMap<String, byte[]> databaseMap;
    private static String databasePath;
    private static RecordManager databaseRecordManager;

    static HashMap<String, ClassInfo> classInfo = new HashMap<>();
    static HashMap<String, TransientClassInfo> transientClassInfo = new HashMap<>();

    public static void init() {
        if (databaseRecordManager != null) {
            return;
        }
        try {
            databaseRecordManager = getOrCreateRecordManager();
        } catch (IOException e) {
            LogUtils.raiseCompilerError("Unable to create or open local data cache.\n" + e);
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

        // Register the classes with the serialiser to enable slightly more concise output.
        serialiser.setRegistrationRequired(true);

        // Let's live dangerously.
        serialiser.setAsmEnabled(false);

        // Serialiser for ClassInfo
        FieldSerializer classInfoSerialiser = new FieldSerializer(serialiser, ClassInfo.class);
        classInfoSerialiser.setFieldsCanBeNull(false);

        // Serialiser for MethodInfo
        FieldSerializer methodInfoSerialiser = new FieldSerializer(serialiser, MethodInfo.class);
        methodInfoSerialiser.setFieldsCanBeNull(false);

        // Serialiser for EffectSet (Just writes the value)
        final EffectSetSerialiser effectSetSerialiser = new EffectSetSerialiser();
        methodInfoSerialiser.getField("effectSet").setClass(EffectSet.class, effectSetSerialiser);

        // To serialise the list of MethodInfo objects inside the ClassInfo.
        CollectionSerializer methodInfoListSerialiser = new CollectionSerializer();
        methodInfoListSerialiser.setElementsCanBeNull(false);
        methodInfoListSerialiser.setElementClass(MethodInfo.class, methodInfoSerialiser);

        // Register the list serialiser with the class serialiser.
        classInfoSerialiser.getField("methodInfos").setClass(LinkedList.class, methodInfoListSerialiser);

        serialiser.register(EffectSet.class, effectSetSerialiser);
        serialiser.register(ClassInfo.class, classInfoSerialiser);
        serialiser.register(MethodInfo.class, methodInfoSerialiser);
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
                LogUtils.raiseCompilerError("Unable to create directory: " + joustDir + " for local data cache.");
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
        if (loadedSymbols.contains(sym)) {
            return;
        }

        byte[] payload = databaseMap.get(sym.fullname.toString());
        if (payload == null) {
            log.warn("No cached info for class {} seems to exist.", sym.fullname.toString());
            return;
        }

        if (sym.classfile == null) {
            log.warn("Unable to load cached info for class {}. No classfile given. Bug?", sym.fullname.toString());
            return;
        }

        log.info("Loaded {} bytes of cached info for class {}", payload.length, sym.fullname.toString());

        @Cleanup UnsafeInput deserialiserInput = new UnsafeInput(payload);
        ClassInfo cInfo = serialiser.readObject(deserialiserInput, ClassInfo.class);

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

        loadedSymbols.add(sym);

        TreeInfoManager.populateFromClassInfo(cInfo);
    }

    static void writeSymbolToDisk(String className, int hash) {
        final ClassInfo cInfo = classInfo.get(className);

        if (cInfo == null) {
            log.warn("Unexpectedly null ClassInfo for {} at cache write.", className);
            return;
        }

        cInfo.setHash(hash);

        // Serialise the ClassInfo object.
        @Cleanup UnsafeOutput serialisedOutput = new UnsafeOutput(INITIAL_BUFFER_SIZE);
        serialiser.writeObject(serialisedOutput, cInfo);

        byte[] buffer = serialisedOutput.toBytes();

        log.debug("Serialised {} using {} bytes", className, buffer.length);

        databaseMap.put(className, buffer);
        try {
            databaseRecordManager.commit();
        } catch (IOException e) {
            LogUtils.raiseCompilerError("IOException flushing to disk cache:" + e);
        }
    }

    /**
     * Register the given EffectSet with the given MethodSymbol. Things so registered will be
     * written to the cache when we exit.
     *
     * @param sym Method symbol to relate the side effects with.
     * @param effectSet The effect set of the provided method symbol's declaration.
     */
    public static void registerMethodSideEffects(MethodSymbol sym, EffectSet effectSet) {
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

        TransientClassInfo tcInfo = transientClassInfo.get(className);
        if (tcInfo == null) {
            tcInfo = new TransientClassInfo();
            transientClassInfo.put(className, tcInfo);
        }
        tcInfo.setSourceFile(((ClassSymbol) sym.owner).sourcefile);
    }
}
