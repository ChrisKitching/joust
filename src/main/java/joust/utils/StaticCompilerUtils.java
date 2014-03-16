package joust.utils;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import joust.tree.annotatedtree.AJCTreeCopier;
import joust.tree.annotatedtree.NonStupidJCTreeCopier;
import joust.tree.annotatedtree.AJCTreeFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileManager;

/**
 * Class to hold the singleton utility classes pilfered from the running javac instance.
 */
public class StaticCompilerUtils {
    // The Trees instance, used to map input elements to their ASTs.
    public static Trees trees;

    // Factory class, internal to the compiler, used to manufacture parse tree nodes.
    public static TreeMaker javacTreeMaker;

    // Factory class for my tree representation.
    public static AJCTreeFactory treeMaker;

    // The compiler's name table.
    public static Names names;

    // The compiler's symbol table.
    public static Symtab symtab;

    // Javac's file manager.
    public static JavaFileManager fileManager;

    // Tree copiers.
    public static AJCTreeCopier treeCopier;
    public static NonStupidJCTreeCopier<Void> javacTreeCopier;

    public static void init(ProcessingEnvironment env) {
        if (isInitialised()) {
            return;
        }

        trees = Trees.instance(env);

        // We typecast the processing environment to the one used by javac. This dirty trick allows
        // us to alter the AST - something not generally possible in annotation processors.
        Context context = ((JavacProcessingEnvironment) env).getContext();
        initWithContext(context);
    }

    public static void initWithContext(Context context) {
        if (isInitialised()) {
            return;
        }

        fileManager = context.get(JavaFileManager.class);
        if (fileManager == null) {
            fileManager = new JavacFileManager(context, true, null);
            context.put(JavaFileManager.class, fileManager);
        }

        javacTreeMaker = TreeMaker.instance(context);
        treeMaker = AJCTreeFactory.instance(context);
        treeCopier = AJCTreeCopier.instance(context);
        javacTreeCopier = new NonStupidJCTreeCopier<>(javacTreeMaker);
        names = Names.instance(context);
        symtab = Symtab.instance(context);
    }

    public static boolean isInitialised() {
        return symtab != null;
    }
}
