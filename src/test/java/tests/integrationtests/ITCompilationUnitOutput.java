package tests.integrationtests;

import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import testutils.BaseIntegrationTestCase;

import javax.tools.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Parameterised JUnit test to ensure the output of the test programs in the resources directory
 * are unchanged after optimisation.
 * The parameter function gets the list of all input files from the resource directory, causing
 * JUnit to produce an instance of the test for each such file, tidily giving us one test per
 * compilation unit without the need for lots of boring boilerplate.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@RunWith(Parameterized.class)
public class ITCompilationUnitOutput {
    public static final String TEST_INPUTS_DIR = "/testinputs/";
    public static final String TEST_SHARED_CLASSES_DIR = "/testutils/";
    public static final String OPT_DIR = "/compilationResults/opt/";
    public static final String UN_OPT_DIR = "/compilationResults/noOpt/";

    public static final String TEST_INPUT_PACKAGE = "testinputs";

    private static final URL sOptOutDir = ITCompilationUnitOutput.class.getResource(OPT_DIR);
    private static final URL sUnOptOutDir = ITCompilationUnitOutput.class.getResource(UN_OPT_DIR);

    private static final String JAVAP = System.getProperty("java.home")+"/../bin/javap";

    // The directory in which classes shared between all unit tests should reside.
    private static File[] mSharedClasses;

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        // Create a File array of all *.java files in the shared classes directory.
        URL sharedClasses = ITCompilationUnitOutput.class.getResource(TEST_SHARED_CLASSES_DIR);
        File[] files = new File(sharedClasses.getFile()).listFiles();
        ArrayList<File> desiredFiles = new ArrayList<>();
        for (File f : files) {
            if (f.getName().endsWith(".java")) {
                desiredFiles.add(f);
            }
        }
        mSharedClasses = desiredFiles.toArray(new File[desiredFiles.size()]);

        URL testInputsUrl = ITCompilationUnitOutput.class.getResource(TEST_INPUTS_DIR);

        File optOutDir = new File(sOptOutDir.getFile());
        File noOptOutdir = new File(sUnOptOutDir.getFile());

        File testInputs = new File(testInputsUrl.getFile());
        File[] testCases = testInputs.listFiles();
        System.out.println(Arrays.toString(testCases));

        LinkedList<Object[]> ret = new LinkedList<>();
        for (int i = 0; i < testCases.length; i++) {
            ret.add(new Object[] {testCases[i], optOutDir, noOptOutdir});
        }

        return ret;
    }

    // A pair of classloaders - one for optimised classes, one for unoptimised classes.
    public static URLClassLoader sOptClassLoader =  new URLClassLoader(new URL[] {sOptOutDir});
    public static URLClassLoader sUnOptClassLoader =  new URLClassLoader(new URL[] {sUnOptOutDir});

    private File mTargetSource;
    private File mOptOutDir;
    private File mNoOptOutDir;

    private String mTestClassName;
    private String mFullyQualifiedTestClassName;

    public ITCompilationUnitOutput(File elementName, File optOutDir, File noOptOutDir) {
        mTargetSource = elementName;
        mOptOutDir = optOutDir;
        mNoOptOutDir = noOptOutDir;

        String n = elementName.getName();
        mTestClassName = n.substring(0, n.lastIndexOf('.'));
        mFullyQualifiedTestClassName = TEST_INPUT_PACKAGE + '.' + mTestClassName;
    }

    /**
     * Compile the given .java with and without optimisation, run both, and ensure the outputs
     * are the same.
     */
    @Test
    public void runTest() {
        log.info("-------------------------------------------------------------------------");
        log.info("- Running compilation output comparism test for {}. -", mTargetSource.getName());
        log.info("-------------------------------------------------------------------------");

        // Create the compiled programs..
        assertTrue(compileTarget(false));
        assertTrue(compileTarget(true));

        // Execute them both and collect their output...
        Class<? extends BaseIntegrationTestCase> optClass = null;
        Class<? extends BaseIntegrationTestCase> noOptClass = null;
        try {
            optClass = (Class<? extends BaseIntegrationTestCase>) sOptClassLoader.loadClass(mFullyQualifiedTestClassName);
            noOptClass = (Class<? extends BaseIntegrationTestCase>) sUnOptClassLoader.loadClass(mFullyQualifiedTestClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Verify we loaded the classes successfully...
        assertNotNull(optClass);
        assertNotNull(noOptClass);

        log.debug("Running no-opt...");
        String noOptOutput = executeCompiledTest(noOptClass);
        log.debug("Returns:\n{}", noOptOutput);

        log.debug("Running opt...");
        String optOutput = executeCompiledTest(optClass);
        log.debug("Returns:\n{}", optOutput);

        // Not using an assertion directly so we can print extra debug information.
        if (!optOutput.equals(noOptOutput)) {
            // Print a bytecode diff for debugging...
            printBytecodeOfTest();

            // ...And fail the test.
            assertTrue(false);
        }

        log.info("---------------------------------------------");
        log.info("- End of compilation output comparism test. -");
        log.info("---------------------------------------------");
    }

    /**
     * Print the bytecode of the failing programs, to aid in debugging.
     */
    private void printBytecodeOfTest() {
        try {
            log.debug("-------------- BYTECODE OF OPTIMISED TESTCASE --------------");
            printBytecodeForDir(sOptOutDir);
            log.debug("-------------- BYTECODE OF UNOPTIMISED TESTCASE --------------");
            printBytecodeForDir(sUnOptOutDir);
        } catch (IOException e) {
            log.debug("IOException dumping bytecode!");
            e.printStackTrace();
        }
    }

    /**
     * Helper method to print the bytecode of the test class for this test at the directory given.
     * Typically called once for optimised builds, and again for the unoptimised build.
     *
     * @param dirName Top-level output directory to look in for the target class.
     * @throws IOException If an IOException is thrown by ProcessBuilder.start trying to call javap.
     */
    private void printBytecodeForDir(URL dirName) throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] cmd = new String[]{JAVAP, "-v", "-classpath", ".", mFullyQualifiedTestClassName};

        Process proc = rt.exec(cmd, null, new File(dirName.getFile()));

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        String s;

        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }
    }

    /**
     * Given a class that extends BaseIntegrationTestCase, run the test and collect the output.
     *
     * @param targetClass
     * @return The output of the test referred to by this class.
     */
    private String executeCompiledTest(Class<? extends BaseIntegrationTestCase> targetClass) {
        BaseIntegrationTestCase t = null;

        try {
            t = targetClass.getConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        assertNotNull(t);

        return t.runTest();
    }

    /**
     * Helper method to compile the target for this test with or without optimisation.
     *
     * @param optimise If true, apply the optimiser. Otherwise, do not apply the optimiser.
     */
    private boolean compileTarget(boolean optimise) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        // Create a list of target files including both the shared files and the target for this test.
        File[] targetFiles = new File[mSharedClasses.length + 1];
        targetFiles[0] = mTargetSource;
        System.arraycopy(mSharedClasses, 0, targetFiles, 1, mSharedClasses.length);
        for (File f: targetFiles) {
            log.warn("File: {}", f);
        }

        Iterable<? extends JavaFileObject> compilationTarget = fileManager.getJavaFileObjects(targetFiles);

        List<String> optionList = new ArrayList<>();
        if (!optimise) {
            optionList.add("-proc:none");
        }
        optionList.add("-source");
        optionList.add("1.7");
        optionList.add("-target");
        optionList.add("1.7");

        // The compilation targets...
        optionList.add("-d");
        if (optimise) {
            optionList.add(mOptOutDir.getPath());
        } else {
            optionList.add(mNoOptOutDir.getPath());
        }

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, optionList, null, compilationTarget);

        log.trace("Calling task.");
        boolean ret = task.call();
        log.trace("Task complete with {} diagnostics to report.", diagnostics.getDiagnostics().size());
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            switch (d.getKind()) {
                case ERROR:
                    ret = false;
                    log.error(d);
                    break;
                case WARNING:
                case MANDATORY_WARNING:
                    log.warn(d);
                    break;
                case NOTE:
                case OTHER:
                    log.info(d);
                    break;
            }
        }

        log.trace("Done.");
        // Perform the compilation task.
        return ret;
    }
}
