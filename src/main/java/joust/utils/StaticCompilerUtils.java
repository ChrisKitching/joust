package joust.utils;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import joust.optimisers.utils.NonStupidTreeCopier;
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

    public static JavaFileManager fileManager;
    public static NonStupidTreeCopier treeCopier;

    public static void init(ProcessingEnvironment env) {
        trees = Trees.instance(env);

        // We typecast the processing environment to the one used by javac. This dirty trick allows
        // us to alter the AST - something not generally possible in annotation processors.
        Context context = ((JavacProcessingEnvironment) env).getContext();

        javacTreeMaker = TreeMaker.instance(context);
        treeMaker = AJCTreeFactory.instance(context);
        treeCopier = new NonStupidTreeCopier<Void>(javacTreeMaker);
        names = Names.instance(context);
        symtab = Symtab.instance(context);
        fileManager = context.get(JavaFileManager.class);
    }
}
