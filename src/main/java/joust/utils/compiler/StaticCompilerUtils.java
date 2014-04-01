package joust.utils.compiler;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import joust.tree.annotatedtree.AJCTreeCopier;
import joust.utils.tree.NonStupidJCTreeCopier;
import joust.tree.annotatedtree.AJCTreeFactory;

import javax.annotation.processing.ProcessingEnvironment;
import com.sun.tools.javac.main.JavaCompiler;
import javax.tools.JavaFileManager;

/**
 * Class to hold the singleton utility classes pilfered from the running javac instance.
 */
public final class StaticCompilerUtils {
    // The Trees instance, used to map input elements to their ASTs.
    public static Trees trees;

    // Factory class, internal to the compiler, used to manufacture parse tree nodes.
    public static TreeMaker javacTreeMaker;

    // Factory class for my tree representation.
    public static AJCTreeFactory treeMaker;

    // The compiler's name table.
    public static Names names;

    public static Types types;

    // The compiler's symbol table.
    public static Symtab symtab;

    // Javac's file manager.
    public static JavaFileManager fileManager;

    // Tree copiers.
    public static AJCTreeCopier treeCopier;
    public static NonStupidJCTreeCopier<Void> javacTreeCopier;

    // The compilation context and compiler.
    public static JavaCompiler javaCompiler;
    public static Context context;

    public static JavacElements javaElements;

    public static void init(ProcessingEnvironment env) {
        if (isInitialised()) {
            return;
        }

        trees = Trees.instance(env);

        // We typecast the processing environment to the one used by javac. This dirty trick allows
        // us to alter the AST - something not generally possible in annotation processors.
        Context con = ((JavacProcessingEnvironment) env).getContext();

        initWithContext(con);
    }

    public static void initWithContext(Context con) {
        if (isInitialised()) {
            return;
        }

        context = con;

        fileManager = con.get(JavaFileManager.class);
        if (fileManager == null) {
            fileManager = new JavacFileManager(con, true, null);
            con.put(JavaFileManager.class, fileManager);
        }

        javaCompiler = JavaCompiler.instance(con);
        javaElements = JavacElements.instance(con);
        javacTreeMaker = TreeMaker.instance(con);
        treeMaker = AJCTreeFactory.instance(con);
        treeCopier = AJCTreeCopier.instance(con);
        javacTreeCopier = new NonStupidJCTreeCopier<>(javacTreeMaker);
        names = Names.instance(con);
        types = Types.instance(con);
        symtab = Symtab.instance(con);
    }

    public static boolean isInitialised() {
        return symtab != null;
    }

    public static void uninit() {
        context = null;
        symtab = null;
        fileManager = null;
        javaCompiler = null;
        javaElements = null;
        javacTreeMaker = null;
        treeMaker = null;
        treeCopier = null;
        javacTreeCopier = null;
        names = null;
        symtab = null;
        trees = null;
    }
}
