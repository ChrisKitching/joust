package joust.tree.annotatedtree;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.analysers.sideeffects.Effects;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import joust.utils.data.JavacListUtils;
import joust.utils.logging.LogUtils;
import joust.utils.ReflectionUtils;
import joust.utils.tree.TreeUtils;
import lombok.Delegate;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;
import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.tree.JCTree.*;

/**
 * A representation of the Java AST. Decorates the nodes in javac's existing AST representation with the extra
 * information required, and slightly tweaks the API to make it suck less (No more fields of type JCTree!)
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class AJCTree implements Tree, Cloneable, JCDiagnostic.DiagnosticPosition {
    // The decorated tree node. Since runtime generics are not available, this field gets shadowed in each concrete
    // subclass of AJCTree, but is required for the delegates to work correctly, and to allow reference to the
    // decorated tree node from an instance of type AJCTree.
    @Delegate @Getter private final JCTree decoratedTree;

    // Get the type annotation for this node, instead of getting the type vaguely related to this node (As the various
    // getType() methods on the various JCTree classes seem to do...).
    public Type getNodeType() {
        return decoratedTree.type;
    }

    // The parent node, if any is meaningfully defined.
    public AJCTree mParentNode;

    protected AJCTree(JCTree tree) {
        decoratedTree = tree;
    }

    /**
     * Replace this node with the given node, if possible.
     */
    public void swapFor(AJCTree replacement) {
        if (mParentNode == null) {
            log.fatal("Unable to swap " + this + " for " + replacement + " - parent was null.");
            return;
        }

        // TODO: This is EEEVIL.

        // Determine which of the fields on the parent are occupied by this node and make the appropriate swap.
        // It is required that no object is inserted more than once into a particular parent.
        Class<?> pClass = mParentNode.getClass();
        Field[] fields = ReflectionUtils.getAllFields(pClass);

        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            try {
                log.debug("Class: {}",fields[i].getType().getCanonicalName());

                // Reparent the new node.
                replacement.mParentNode = mParentNode;
                if (this instanceof AJCStatement) {
                    ((AJCStatement) replacement).enclosingBlock = ((AJCStatement) this).enclosingBlock;
                }

                // For non-list fields, just swap in parent.
                if(!"com.sun.tools.javac.util.List".equals(fields[i].getType().getCanonicalName())) {
                    if (fields[i].get(mParentNode) == this) {
                        // Target found!
                        fields[i].set(mParentNode, replacement);

                        // Push this change onto the JCTree, as well...
                        Class<? extends JCTree> underlyingClass = mParentNode.decoratedTree.getClass();
                        Field targetField = ReflectionUtils.findField(underlyingClass, fields[i].getName());
                        targetField.set(mParentNode.decoratedTree, replacement.decoratedTree);
                        log.debug("Swapping {} for {} in field {}", this, replacement, fields[i].getName());
                        return;
                    }

                    continue;
                }

                // For list fields, swap inside the list...
                @SuppressWarnings("unchecked")
                List<AJCTree> theList = (List<AJCTree>) fields[i].get(mParentNode);
                if (theList.contains(this)) {
                    theList = JavacListUtils.replace(theList, this, replacement);
                    fields[i].set(mParentNode, theList);

                    // Push this change onto the JCTree, as well...
                    Class<? extends JCTree> underlyingClass = mParentNode.decoratedTree.getClass();
                    Field targetField = ReflectionUtils.findField(underlyingClass, fields[i].getName());

                    @SuppressWarnings("unchecked")
                    List<JCTree> realList = (List<JCTree>) targetField.get(mParentNode.decoratedTree);
                    realList = JavacListUtils.replace(realList, decoratedTree, replacement.decoratedTree);
                    targetField.set(mParentNode.decoratedTree, realList);

                    return;
                }
            } catch (IllegalAccessException e) {
                // Ostensibly can never happen, because setAccessible is called...
                log.fatal("IllegalAccessException accessing field " + fields[i] + " on " + pClass.getCanonicalName(), e);
            } catch (NoSuchFieldException e) {
                log.fatal("NoSuchFieldException accessing field " + fields[i] + " on " + pClass.getCanonicalName(), e);
            }
        }

        log.fatal("Unable to perform swap of {} for {} in {}", this, replacement, mParentNode);
    }

    /**
     * Unwrap a list of tree nodes and produce a list of their underlying AST nodes.
     */
    @SuppressWarnings("unchecked")
    public static<T extends JCTree, Q extends AJCTree> List<T> unwrap(List<Q> nodes) {
        List<T> realNodes = List.nil();

        for (Q node : nodes) {
            realNodes = realNodes.prepend((T) node.getDecoratedTree());
        }

        return realNodes.reverse();
    }

    @Override
    public String toString() {
        return decoratedTree.toString();
    }

    /**
     * Base class for nodes that can have side effects - avoids allocating effect sets for things like type identifiers.
     */
    public abstract static class AJCEffectAnnotatedTree extends AJCTree {
        public Effects effects = new Effects(EffectSet.ALL_EFFECTS);

        // The results of LVA at this node.
        public Set<VarSymbol> liveVariables;

        protected AJCEffectAnnotatedTree(JCTree tree) {
            super(tree);
        }

        @Override
        public String toString() {
            return super.toString() + ':' + effects.getEffectSet();
        }
    }

    public abstract static class AJCStatement extends AJCEffectAnnotatedTree implements StatementTree {
        @Delegate @Getter private final JCStatement decoratedTree;

        // The block in which this statement resides, if any.
        protected AJCBlock enclosingBlock;

        public AJCBlock getEnclosingBlock() {
            if (enclosingBlock != null) {
                return enclosingBlock;
            }

            if (mParentNode instanceof AJCStatement) {
                return ((AJCStatement) mParentNode).getEnclosingBlock();
            } else if (mParentNode instanceof AJCExpressionTree) {
                return ((AJCExpressionTree) mParentNode).getEnclosingBlock();
            }

            log.fatal("Failed to find enclosing block for statement: " + this);
            return null;
        }

        // Since a statement is always in a block, we can provide a specialised swap function and avoid the evilness.
        @Override
        public void swapFor(AJCTree replacement) {
            if (replacement instanceof AJCStatement) {
                // Swap in the enclosing block. Simplez!
                enclosingBlock.swap(this, (AJCStatement) replacement);
            } else {
                log.fatal("Attempt to swap an AJCStatement for a non-statement: {}:{}", replacement, replacement.getClass().getCanonicalName());
                throw new UnsupportedOperationException("Attempt to swap an AJCStatement for a non-statement!");
            }
        }

        protected AJCStatement(JCStatement tree) {
            super(tree);
            decoratedTree = tree;
        }

        public boolean isEmptyStatement() {
            return false;
        }
    }

    /**
     * Marker interface for expressions.
     */
    public interface AJCExpression {}

    public abstract static class AJCExpressionTree extends AJCEffectAnnotatedTree implements ExpressionTree, AJCExpression {
        @Delegate @Getter private final JCExpression decoratedTree;

        /**
         * Get the statement in which this expression resides.
         */
        public AJCStatement getEnclosingStatement() {
            log.debug("getEnclosingStatement on {} parent {}", this, mParentNode);
            if (mParentNode == null) {
                log.debug("PANIC");
            }
            AJCTree parent = mParentNode;
            while (!(parent instanceof AJCStatement) && parent.mParentNode != null) {
                parent = parent.mParentNode;
            }

            if (!(parent instanceof AJCStatement)) {
                log.fatal("Unable to find enclosing statement for: " + this);
                return null;
            }

            return (AJCStatement) parent;
        }

        /**
         * Get the block in which the statement containing this expression is contained. Handy for finding the place to
         * stick new temporary variables.
         */
        public AJCBlock getEnclosingBlock() {
            return getEnclosingStatement().getEnclosingBlock();
        }

        protected AJCExpressionTree(JCExpression tree) {
            super(tree);
            decoratedTree = tree;
        }

        public boolean isEmptyExpression() {
            return false;
        }
    }

    public static class AJCConditional extends AJCExpressionTree implements ConditionalExpressionTree {
        @Delegate @Getter private final JCConditional decoratedTree;

        public AJCExpressionTree cond;
        public AJCExpressionTree truepart;
        public AJCExpressionTree falsepart;

        protected AJCConditional(JCConditional tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCConditional(JCConditional tree, AJCExpressionTree c, AJCExpressionTree tPart, AJCExpressionTree fPart) {
            this(tree);
            cond = c;
            truepart = tPart;
            falsepart = fPart;
        }
    }

    public static class AJCCall extends AJCSymbolRefTree<MethodSymbol> implements MethodInvocationTree {
        @Delegate @Getter private final JCMethodInvocation decoratedTree;

        @Delegate
        public AJCSymbolRefTree<MethodSymbol> meth;
        public List<AJCExpressionTree> args;

        protected AJCCall(JCMethodInvocation tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCCall(JCMethodInvocation tree, AJCSymbolRefTree<MethodSymbol> m, List<AJCExpressionTree> arg) {
            this(tree);
            meth = m;
            args = arg;
        }
    }

    public static class AJCNewClass extends AJCSymbolRefTree<MethodSymbol> implements NewClassTree {
        @Delegate @Getter private final JCNewClass decoratedTree;

        public AJCSymbolRefTree<ClassSymbol> clazz;
        public List<AJCExpressionTree> args;
        public AJCClassDecl def;

        protected AJCNewClass(JCNewClass tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCNewClass(JCNewClass tree, AJCSymbolRefTree<ClassSymbol> clasz,
                             List<AJCExpressionTree> arg, AJCClassDecl defn) {
            this(tree);
            clazz = clasz;
            args = arg;
            def = defn;

        }

        @Override
        public MethodSymbol getTargetSymbol() {
            return (MethodSymbol) decoratedTree.constructor;
        }
    }

    public static class AJCClassDecl extends AJCTree implements ClassTree {
        @Delegate @Getter private final JCClassDecl decoratedTree;

        public AJCModifiers mods;
        /** the classes this class extends */
        public AJCExpressionTree extending;
        /** the interfaces implemented by this class */
        public List<AJCExpressionTree> implementing;

        // Fields defined in this class.
        public List<AJCVariableDecl> fields;

        // Methods defined in this class.
        public List<AJCMethodDecl> methods;

        // Classes defined in this class.
        public List<AJCClassDecl> classes;

        protected AJCClassDecl(JCClassDecl tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCClassDecl(JCClassDecl tree, AJCModifiers mods,
                              AJCExpressionTree extending, List<AJCExpressionTree> implementing, List<AJCVariableDecl> fields,
                              List<AJCMethodDecl> methods, List<AJCClassDecl> classes) {
            this(tree);
            this.mods = mods;
            this.extending = extending;
            this.implementing = implementing;
            this.fields = fields;
            this.methods = methods;
            this.classes = classes;
        }

        public ClassSymbol getSym() {
            return decoratedTree.sym;
        }
    }

    public static class AJCMethodDecl extends AJCTree implements MethodTree, AJCSymbolRef<MethodSymbol> {
        @Delegate @Getter private final JCMethodDecl decoratedTree;

        public AJCModifiers mods;
        /** type of method return value */
        public AJCTypeExpression restype;
        /** receiver parameter */
        public AJCVariableDecl recvparam;
        /** value parameters */
        public List<AJCVariableDecl> params;
        /** exceptions thrown by this method */
        public List<AJCExpressionTree> thrown;
        /** statements in the method */
        public AJCBlock body;
        /** default value, for annotation types */
        public AJCExpressionTree defaultValue;

        // All symbols ever live within the body of this method.
        public Set<VarSymbol> everLive;

        protected AJCMethodDecl(JCMethodDecl tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCMethodDecl(JCMethodDecl tree, AJCModifiers mods, AJCTypeExpression restype,
                               AJCVariableDecl recvparam, List<AJCVariableDecl> params, List<AJCExpressionTree> thrown, AJCBlock body,
                               AJCExpressionTree defaultValue)
        {
            this(tree);
            this.mods = mods;
            this.restype = restype;
            this.params = params;
            this.recvparam = recvparam;
            this.thrown = thrown;
            this.body = body;
            this.defaultValue = defaultValue;
        }

        @Override
        public MethodSymbol getTargetSymbol() {
            return decoratedTree.sym;
        }
    }

    public static class AJCVariableDecl extends AJCStatement implements VariableTree, AJCSymbolRef<VarSymbol> {
        @Delegate @Getter private final JCVariableDecl decoratedTree;

        public AJCModifiers mods;
        /** type of the variable */
        public AJCTypeExpression vartype;
        /** variable's initial value */
        @Getter protected AJCExpressionTree init = new AJCEmptyExpression();

        protected AJCVariableDecl(JCVariableDecl tree) {
            super(tree);
            decoratedTree = tree;
        }


        protected AJCVariableDecl(JCVariableDecl tree, AJCModifiers mods, AJCTypeExpression vartype,
                                 AJCExpressionTree init) {
            this(tree);
            this.mods = mods;
            this.vartype = vartype;
            this.init = init;
        }

        public void setInit(AJCExpressionTree expr) {
            init = expr;
            decoratedTree.init = expr.decoratedTree;
        }

        @Override
        public VarSymbol getTargetSymbol() {
            return decoratedTree.sym;
        }
    }

    /**
     * An expression that does nothing.
     */
    public static class AJCEmptyExpression extends AJCExpressionTree {
        protected AJCEmptyExpression() {
            super(null);
            effects = new Effects(EffectSet.NO_EFFECTS);
        }

        @Override
        public boolean isEmptyExpression() {
            return true;
        }

        @Override
        public String toString() {
            return "/* */";
        }
    }

    public static class AJCSkip extends AJCStatement implements EmptyStatementTree {
        @Delegate @Getter private final JCSkip decoratedTree;

        protected AJCSkip(JCSkip tree) {
            super(tree);
            decoratedTree = tree;
            effects = new Effects(EffectSet.NO_EFFECTS);
        }

        @Override
        public boolean isEmptyStatement() {
            return true;
        }
    }

    public static class AJCBlock extends AJCStatement implements BlockTree {
        @Delegate @Getter private final JCBlock decoratedTree;

        // The method in which this block resides.
        public AJCMethodDecl enclosingMethod;

        public List<AJCStatement> stats;

        protected AJCBlock(JCBlock tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCBlock(JCBlock tree, List<AJCStatement> statements) {
            this(tree);
            stats = statements;
        }

        /**
         * Remove the given statement from this block, and reflect the update in the underlying Javac AST.
         */
        public void remove(AJCStatement statement) {
            stats = JavacListUtils.removeElement(stats, statement);
            decoratedTree.stats = JavacListUtils.removeElement(decoratedTree.stats, statement.decoratedTree);
        }

        /**
         * Add the given statement at the specified index in the block, reflecting the update in underlying decorated block.
         */
        public void insert(AJCStatement statement, int index) {
            statement.enclosingBlock = this;
            statement.mParentNode = this;
            if (statement instanceof AJCBlock) {
                ((AJCBlock) statement).enclosingMethod = enclosingMethod;
            }

            stats = JavacListUtils.addAtIndex(stats, index, statement);
            decoratedTree.stats = JavacListUtils.addAtIndex(decoratedTree.stats, index, statement.decoratedTree);
        }
        public void insert(List<AJCStatement> statements, int index) {
            // Compute early since the splicing is destructive.
            List<JCStatement> unwrapped = AJCTree.unwrap(statements);

            stats = JavacListUtils.addAtIndex(stats, index, statements);
            for (AJCStatement st : stats) {
                st.enclosingBlock = this;
                st.mParentNode = this;
                if (st instanceof AJCBlock) {
                    ((AJCBlock) st).enclosingMethod = enclosingMethod;
                }
            }

            decoratedTree.stats = JavacListUtils.addAtIndex(decoratedTree.stats, index, unwrapped);
        }

        private int indexOfOrFail(AJCStatement node) {
            int index = stats.indexOf(node);
            if (index == -1) {
                log.fatal("Unable to locate target statement: " + node + " in block:\n" + this);
            }

            return index;
        }
        /**
         * Insert a statement before a given statement.
         */
        public void insertBefore(AJCStatement before, AJCStatement insert) {
            int index = indexOfOrFail(before);
            insert(insert, index);
        }
        public void insertBefore(AJCStatement before, List<AJCStatement> insert) {
            int index = indexOfOrFail(before);
            insert(insert, index);
        }

        /**
         * Insert a statement after a given statement.
         */
        public void insertAfter(AJCStatement after, AJCStatement insert) {
            int index = indexOfOrFail(after);
            insert(insert, index + 1);
        }
        public void insertAfter(AJCStatement after, List<AJCStatement> insert) {
            int index = indexOfOrFail(after);
            insert(insert, index + 1);
        }

        public int indexOf(AJCStatement statement) {
            return stats.indexOf(statement);
        }

        /**
         * Swap the given statement for the other given statement.
         */
        public void swap(AJCStatement target, AJCStatement replacement) {
            replacement.mParentNode = this;
            replacement.enclosingBlock = this;
            stats = JavacListUtils.replace(stats, target, replacement);
            decoratedTree.stats = JavacListUtils.replace(decoratedTree.stats, target.decoratedTree, replacement.decoratedTree);
        }
    }

    public static class AJCDoWhileLoop extends AJCStatement implements DoWhileLoopTree {
        @Delegate @Getter private final JCDoWhileLoop decoratedTree;

        public AJCBlock body;
        public AJCExpressionTree cond;

        protected AJCDoWhileLoop(JCDoWhileLoop tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCDoWhileLoop(JCDoWhileLoop tree, AJCBlock body, AJCExpressionTree cond) {
            this(tree);
            this.body = body;
            this.cond = cond;
        }
    }

    public static class AJCWhileLoop extends AJCStatement implements WhileLoopTree {
        @Delegate @Getter private final JCWhileLoop decoratedTree;

        public AJCExpressionTree cond;
        public AJCBlock body;

        protected AJCWhileLoop(JCWhileLoop tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCWhileLoop(JCWhileLoop tree, AJCExpressionTree cond, AJCBlock body) {
            this(tree);
            this.body = body;
            this.cond = cond;
        }
    }

    public static class AJCForLoop extends AJCStatement implements ForLoopTree {
        @Delegate @Getter private final JCForLoop decoratedTree;

        public List<AJCStatement> init;
        public AJCExpressionTree cond;
        public List<AJCExpressionStatement> step;
        public AJCBlock body;

        protected AJCForLoop(JCForLoop tree) {
            super(tree);
            decoratedTree = tree;
        }


        protected AJCForLoop(JCForLoop tree, List<AJCStatement> init, AJCExpressionTree cond, List<AJCExpressionStatement> update, AJCBlock body) {
            this(tree);
            this.init = init;
            this.cond = cond;
            this.step = update;
            this.body = body;
        }
    }

    public static class AJCLabeledStatement extends AJCStatement implements LabeledStatementTree {
        @Delegate @Getter private final JCLabeledStatement decoratedTree;

        public AJCStatement body;

        protected AJCLabeledStatement(JCLabeledStatement tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCLabeledStatement(JCLabeledStatement tree, AJCStatement body) {
            this(tree);
            this.body = body;
        }
    }

    public static class AJCSwitch extends AJCStatement implements SwitchTree {
        @Delegate @Getter private final JCSwitch decoratedTree;

        public AJCExpressionTree selector;
        public List<AJCCase> cases;

        protected AJCSwitch(JCSwitch tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCSwitch(JCSwitch tree, AJCExpressionTree selector, List<AJCCase> cases) {
            this(tree);
            this.selector = selector;
            this.cases = cases;
        }
    }

    public static class AJCCase extends AJCStatement implements CaseTree {
        @Delegate @Getter private final JCCase decoratedTree;

        // The empty expression here represents the default case.
        public AJCExpressionTree pat = new AJCEmptyExpression();
        public List<AJCStatement> stats;

        protected AJCCase(JCCase tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCCase(JCCase tree, AJCExpressionTree pat, List<AJCStatement> stats) {
            this(tree);
            this.pat = pat;
            this.stats = stats;
        }
    }

    public static class AJCSynchronized extends AJCStatement implements SynchronizedTree {
        @Delegate @Getter private final JCSynchronized decoratedTree;

        public AJCExpressionTree lock;
        public AJCBlock body;

        protected AJCSynchronized(JCSynchronized tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCSynchronized(JCSynchronized tree, AJCExpressionTree lock, AJCBlock body) {
            this(tree);
            this.lock = lock;
            this.body = body;
        }
    }

    public static class AJCTry extends AJCStatement implements TryTree {
        @Delegate @Getter private final JCTry decoratedTree;

        public AJCBlock body;
        public List<AJCCatch> catchers;
        public AJCBlock finalizer;

        protected AJCTry(JCTry tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCTry(JCTry tree, AJCBlock body, List<AJCCatch> catchers, AJCBlock finalizer) {
            this(tree);
            this.body = body;
            this.catchers = catchers;
            this.finalizer = finalizer;
        }
    }

    public static class AJCCatch extends AJCEffectAnnotatedTree implements CatchTree {
        @Delegate @Getter private final JCCatch decoratedTree;

        public AJCVariableDecl param;
        public AJCBlock body;

        protected AJCCatch(JCCatch tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCCatch(JCCatch tree, AJCVariableDecl param, AJCBlock body) {
            this(tree);
            this.param = param;
            this.body = body;
        }
    }

    public static class AJCIf extends AJCStatement implements IfTree {
        @Delegate @Getter private final JCIf decoratedTree;

        public AJCExpressionTree cond;
        public AJCBlock thenpart;
        public AJCBlock elsepart;

        protected AJCIf(JCIf tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCIf(JCIf tree, AJCExpressionTree cond, AJCBlock thenpart, AJCBlock elsepart) {
            this(tree);
            this.cond = cond;
            this.thenpart = thenpart;
            this.elsepart = elsepart;
        }
    }

    public static class AJCExpressionStatement extends AJCStatement implements ExpressionStatementTree {
        @Delegate @Getter private final JCExpressionStatement decoratedTree;

        public AJCExpressionTree expr;

        protected AJCExpressionStatement(JCExpressionStatement tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCExpressionStatement(JCExpressionStatement tree, AJCExpressionTree expr) {
            this(tree);
            this.expr = expr;
        }
    }

    public static class AJCBreak extends AJCStatement implements BreakTree {
        @Delegate @Getter private final JCBreak decoratedTree;

        public AJCTree target;

        protected AJCBreak(JCBreak tree) {
            super(tree);
            decoratedTree = tree;
        }
    }

    public static class AJCContinue extends AJCStatement implements ContinueTree {
        @Delegate @Getter private final JCContinue decoratedTree;

        public AJCTree target;

        protected AJCContinue(JCContinue tree) {
            super(tree);
            decoratedTree = tree;
        }
    }

    public static class AJCReturn extends AJCStatement implements ReturnTree {
        @Delegate @Getter private final JCReturn decoratedTree;

        public AJCExpressionTree expr;

        protected AJCReturn(JCReturn tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCReturn(JCReturn tree, AJCExpressionTree expr) {
            this(tree);
            this.expr = expr;
        }
    }

    public static class AJCThrow extends AJCStatement implements ThrowTree {
        @Delegate @Getter private final JCThrow decoratedTree;

        public AJCExpressionTree expr;

        protected AJCThrow(JCThrow tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCThrow(JCThrow tree, AJCExpressionTree expr) {
            this(tree);
            this.expr = expr;
        }
    }

    public static class AJCNewArray extends AJCExpressionTree implements NewArrayTree {
        @Delegate @Getter private final JCNewArray decoratedTree;

        public AJCTypeExpression elemtype;
        public List<AJCExpressionTree> dims;
        // type annotations on inner-most component
        public List<AJCAnnotation> annotations = List.nil();
        public List<AJCExpressionTree> elems;

        protected AJCNewArray(JCNewArray tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCNewArray(JCNewArray tree, AJCTypeExpression elemtype, List<AJCExpressionTree> dims, List<AJCExpressionTree> elems) {
            this(tree);
            this.elemtype = elemtype;
            this.dims = dims;
            this.elems = elems;
        }
    }

    public static class AJCAssign extends AJCExpressionTree implements AssignmentTree, AJCSymbolRef<VarSymbol> {
        @Delegate @Getter private final JCAssign decoratedTree;

        public AJCSymbolRefTree<VarSymbol> lhs;
        public AJCExpressionTree rhs;

        protected AJCAssign(JCAssign tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCAssign(JCAssign tree, AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs) {
            this(tree);
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public VarSymbol getTargetSymbol() {
            log.debug("GetTargetSymbol on: {}", this);
            return lhs.getTargetSymbol();
        }
    }

    public abstract static class AJCOperatorExpression extends AJCExpressionTree {
        protected AJCOperatorExpression(JCExpression tree) {
            super(tree);
        }

        // Since javac provides no nice class for expressions with operators, instead redefining an identical field in
        // multiple classes, we have to do some stupidity to tidy it up here...
        public void setOperator(Symbol op) {
            JCExpression tree = getDecoratedTree();
            if (tree instanceof JCAssignOp) {
                ((JCAssignOp) tree).operator = op;
            } else if (tree instanceof JCBinary) {
                ((JCBinary) tree).operator = op;
            } else if (tree instanceof JCUnary) {
                ((JCUnary) tree).operator = op;
            } else {
                log.fatal("Unknown operator expression type: " + tree.getClass().getCanonicalName());
                return;
            }
        }

        public abstract Symbol getOperator();
    }

    public static class AJCAssignOp extends AJCOperatorExpression implements CompoundAssignmentTree, AJCSymbolRef<VarSymbol> {
        @Delegate @Getter private final JCAssignOp decoratedTree;

        @Delegate
        public AJCSymbolRefTree<VarSymbol> lhs;
        public AJCExpressionTree rhs;

        protected AJCAssignOp(JCAssignOp tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCAssignOp(JCAssignOp tree, AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs) {
            this(tree);
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    public static class AJCUnary extends AJCOperatorExpression implements UnaryTree {
        @Delegate @Getter private final JCUnary decoratedTree;

        public AJCExpressionTree arg;

        protected AJCUnary(JCUnary tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCUnary(JCUnary tree, AJCExpressionTree arg) {
            this(tree);
            this.arg = arg;
        }
    }

    /**
     * A unary assignment, such as a++.
     */
    public static class AJCUnaryAsg extends AJCOperatorExpression implements UnaryTree, AJCSymbolRef<VarSymbol> {
        @Delegate @Getter private final JCUnary decoratedTree;

        @Delegate
        public AJCSymbolRefTree<VarSymbol> arg;

        protected AJCUnaryAsg(JCUnary tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCUnaryAsg(JCUnary tree, AJCSymbolRefTree<VarSymbol> arg) {
            this(tree);
            this.arg = arg;
        }
    }

    public static class AJCBinary extends AJCOperatorExpression implements BinaryTree {
        @Delegate @Getter private final JCBinary decoratedTree;

        public AJCExpressionTree lhs;
        public AJCExpressionTree rhs;

        protected AJCBinary(JCBinary tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCBinary(JCBinary tree, AJCExpressionTree lhs, AJCExpressionTree rhs) {
            this(tree);
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    public static class AJCTypeCast extends AJCExpressionTree implements TypeCastTree {
        @Delegate @Getter private final JCTypeCast decoratedTree;

        public AJCTypeExpression clazz;
        public AJCExpressionTree expr;

        protected AJCTypeCast(JCTypeCast tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCTypeCast(JCTypeCast tree, AJCTypeExpression clazz, AJCExpressionTree expr) {
            this(tree);
            this.clazz = clazz;
            this.expr = expr;
        }
    }

    public static class AJCInstanceOf extends AJCExpressionTree implements InstanceOfTree, AJCSymbolRef<VarSymbol> {
        @Delegate @Getter private final JCInstanceOf decoratedTree;

        @Delegate
        public AJCSymbolRefTree<VarSymbol> expr;
        public AJCSymbolRef<TypeSymbol> clazz;

        protected AJCInstanceOf(JCInstanceOf tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCInstanceOf(JCInstanceOf tree, AJCSymbolRefTree<VarSymbol> expr, AJCSymbolRef<TypeSymbol> clazz) {
            this(tree);
            this.expr = expr;
            this.clazz = clazz;
        }
    }

    public static class AJCArrayAccess extends AJCSymbolRefTree<VarSymbol> implements ArrayAccessTree {
        @Delegate @Getter private final JCArrayAccess decoratedTree;

        public AJCExpressionTree indexed;
        public AJCExpressionTree index;

        protected AJCArrayAccess(JCArrayAccess tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCArrayAccess(JCArrayAccess tree, AJCExpressionTree indexed, AJCExpressionTree index) {
            this(tree);
            this.indexed = indexed;
            this.index = index;
        }

        @Override
        public VarSymbol getTargetSymbol() {
            // Since you can do things like `f()[3]`, the field can't be a SymbolRefTree.
            if (indexed instanceof AJCSymbolRef) {
                Symbol sym = ((AJCSymbolRef) indexed).getTargetSymbol();
                if (sym instanceof VarSymbol) {
                    return (VarSymbol) sym;
                }
            }

            log.warn("Unconventional array access {} returning null symbol!", this);
            return null;
        }
    }

    public static class AJCFieldAccess<T extends Symbol> extends AJCSymbolRefTree<T> implements MemberSelectTree {
        @Delegate @Getter private final JCFieldAccess decoratedTree;

        // The lhs of the dot... We're selecting <selected>.<name>. So literals...
        public AJCExpressionTree selected;

        protected AJCFieldAccess(JCFieldAccess tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCFieldAccess(JCFieldAccess tree, AJCExpressionTree selected) {
            this(tree);
            this.selected = selected;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getTargetSymbol() {
            return (T) decoratedTree.sym;
        }
    }

    public static class AJCIdent<T extends Symbol> extends AJCSymbolRefTree<T> implements IdentifierTree {
        @Delegate @Getter private final JCIdent decoratedTree;

        protected AJCIdent(JCIdent tree) {
            super(tree);
            decoratedTree = tree;
        }

        @SuppressWarnings("unchecked")
        public T getSym() {
            return (T) decoratedTree.sym;
        }

        @Override
        public T getTargetSymbol() {
            return getSym();
        }
    }

    public static class AJCLiteral extends AJCExpressionTree implements LiteralTree {
        private static final String INVALID_ARGUMENT_TYPE = "Invalid literal type! Expected {} got {}";
        private static final String INVALID_ARGUMENT_TYPE_SIMPLE = "Invalid literal type!";

        /**
         * Sanitise a literal value, returning the value to be passed to the underlying tree node after applying whatever
         * stupid transformations are called for by the tree representation Javac is using this week.
         * Throws an IllegalArgumentException if the input TypeTag is inconsistent with the input value.
         *
         * @param tag TypeTag of the literal being considered.
         * @param value Value of the literal being considered.
         * @return The value that is necessary for creating a JCLiteral to hold this information.
         */
        public static Object sanitiseLiteralValue(TypeTag tag, Object value) {
            if (tag == BOOLEAN) {
                // Booleans are represented as integer 1/0 in the obvious way.
                if (value instanceof Boolean) {
                    if ((Boolean) value) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            } else if (tag == CHAR) {
                // Chars are represented as integers for... some reason...
                if (value instanceof Character) {
                    // Yes. This *is* how javac does it.
                    return (int) value.toString().charAt(0);
                }
            }

            // A quick check to verify that the type of the input object matches the TypeTag.
            // Apparently the javac authors have never heard of static polymorphism...
            switch (tag) {
                case CLASS:
                    // A String literal. Yes. Apparently they thought that was a good name for the tag, too.
                    if (!(value instanceof String)) {
                        log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                        throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
                    }
                    break;
                case INT:
                    if (!(value instanceof Integer)) {
                        log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                        throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
                    }
                    break;
                case LONG:
                    if (!(value instanceof Long)) {
                        log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                        throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
                    }
                    break;
                case BYTE:
                    if (!(value instanceof Byte)) {
                        log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                        throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
                    }
                    break;
                case CHAR:
                    // ... Because chars are integers, remember...
                    if (!(value instanceof Integer)) {
                        log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                        throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
                    }
                    break;
                case DOUBLE:
                    if (!(value instanceof Double)) {
                        log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                        throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
                    }
                    break;
                case FLOAT:
                    if (!(value instanceof Float)) {
                        log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                        throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
                    }
                    break;
                case SHORT:
                    if (!(value instanceof Short)) {
                        log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                        throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
                    }
                    break;
                case BOOLEAN:
                    // Booleans are secretly ints...
                    if (!(value instanceof Integer)) {
                        log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                        throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
                    }
                    break;
                default:
                    log.fatal(INVALID_ARGUMENT_TYPE, tag, value.getClass().getSimpleName());
                    throw new IllegalArgumentException(INVALID_ARGUMENT_TYPE_SIMPLE);
            }

            return value;
        }

        @Delegate @Getter private final JCLiteral decoratedTree;

        protected AJCLiteral(JCLiteral tree) {
            super(tree);
            decoratedTree = tree;
            effects = new Effects(EffectSet.NO_EFFECTS);
        }

        /**
         * Update the value to the given value.
         */
        public void setValue(Object value) {
            TypeTag tag = decoratedTree.typetag;

            value = sanitiseLiteralValue(tag, value);

            decoratedTree.value = value;
        }
    }

    /**
     * Base class of type expressions - may not hold an effect annotation.
     */
    public abstract static class AJCTypeExpression extends AJCTree implements ExpressionTree, AJCExpression, AJCSymbolRef<TypeSymbol> {
        @Delegate @Getter private final JCExpression decoratedTree;

        protected AJCTypeExpression(JCExpression tree) {
            super(tree);
            decoratedTree = tree;
        }
    }

    public static class AJCPrimitiveTypeTree extends AJCTypeExpression implements PrimitiveTypeTree {
        @Delegate @Getter private final JCPrimitiveTypeTree decoratedTree;

        protected AJCPrimitiveTypeTree(JCPrimitiveTypeTree tree) {
            super(tree);
            decoratedTree = tree;
        }

        @Override
        public TypeSymbol getTargetSymbol() {
            return TreeUtils.typeKindToType(decoratedTree.getPrimitiveTypeKind()).tsym;
        }
    }

    /**
     * Non-primitive type tree...
     */
    public static class AJCObjectTypeTree extends AJCTypeExpression {
        @Delegate @Getter private final AJCSymbolRefTree<TypeSymbol> underlyingSymbol;

        protected AJCObjectTypeTree(AJCSymbolRefTree<TypeSymbol> tree) {
            super(tree.getDecoratedTree());
            underlyingSymbol = tree;
        }
    }

    /**
     * Class for array types. Represents an array of type elemtype.
     */
    public static class AJCArrayTypeTree extends AJCTypeExpression implements ArrayTypeTree {
        @Delegate @Getter private final JCArrayTypeTree decoratedTree;

        public AJCTypeExpression elemtype;

        protected AJCArrayTypeTree(JCArrayTypeTree tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCArrayTypeTree(JCArrayTypeTree tree, AJCTypeExpression elemtype) {
            this(tree);
            this.elemtype = elemtype;
        }

        @Override
        public TypeSymbol getTargetSymbol() {
            return elemtype.getTargetSymbol();
        }
    }

    public static class AJCTypeUnion extends AJCTypeExpression implements UnionTypeTree {
        @Delegate @Getter private final JCTypeUnion decoratedTree;

        public List<AJCTypeExpression> alternatives;

        protected AJCTypeUnion(JCTypeUnion tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCTypeUnion(JCTypeUnion tree, List<AJCTypeExpression> components) {
            this(tree);
            alternatives = components;
        }

        /**
         * By convention, just return the first one. If type unions get more widespread adoption in Java more work needed
         * here... (ie. The ability to return sets of TypeSymbols).
         */
        @Override
        public TypeSymbol getTargetSymbol() {
            if (!alternatives.isEmpty()) {
                return alternatives.get(0).getTargetSymbol();
            }

            return null;
        }
    }

    public static class AJCAnnotation extends AJCExpressionTree implements AnnotationTree {
        @Delegate @Getter private final JCAnnotation decoratedTree;

        // TODO: Type expression?
        public AJCTree annotationType;
        public List<AJCExpressionTree> args;

        protected AJCAnnotation(JCAnnotation tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCAnnotation(JCAnnotation tree, AJCTree annotationType, List<AJCExpressionTree> args) {
            this(tree);
            this.annotationType = annotationType;
            this.args = args;
        }
    }

    public static class AJCModifiers extends AJCTree {
        @Delegate @Getter private final JCModifiers decoratedTree;

        public List<AJCAnnotation> annotations;

        protected AJCModifiers(JCModifiers tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCModifiers(JCModifiers tree, List<AJCAnnotation> annotations) {
            this(tree);
            this.annotations = annotations;
        }
    }

    // TODO: What does this even represent? o.0
    public static class AJCAnnotatedType extends AJCTypeExpression implements AnnotatedTypeTree {
        @Delegate @Getter private final JCAnnotatedType decoratedTree;

        public AJCTypeExpression underlyingType;

        protected AJCAnnotatedType(JCAnnotatedType tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCAnnotatedType(JCAnnotatedType tree, AJCTypeExpression underlyingType) {
            this(tree);
            this.underlyingType = underlyingType;
        }

        @Override
        public TypeSymbol getTargetSymbol() {
            return underlyingType.getTargetSymbol();
        }
    }

    public static class AJCErroneous extends AJCExpressionTree {
        @Delegate @Getter private final JCErroneous decoratedTree;

        protected AJCErroneous(JCErroneous tree) {
            super(tree);
            decoratedTree = tree;
        }
    }

    public static class AJCLetExpr extends AJCExpressionTree {
        @Delegate @Getter private final LetExpr decoratedTree;

        public List<AJCVariableDecl> defs;
        public AJCExpressionTree expr;

        protected AJCLetExpr(LetExpr tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCLetExpr(LetExpr tree, List<AJCVariableDecl> defs, AJCExpressionTree expr) {
            this(tree);
            this.defs = defs;
            this.expr = expr;
        }
    }

    /**
     * An abstract class for nodes that represent references to symbols - Idents and FieldAccesses, mostly.
     */
    public abstract static class AJCSymbolRefTree<T extends Symbol> extends AJCExpressionTree implements AJCSymbolRef<T> {
        protected AJCSymbolRefTree(JCExpression tree) {
            super(tree);
        }

        @Override
        public abstract T getTargetSymbol();
    }

    /**
     * An interface for nodes that refer to a symbol.
     */
    public interface AJCSymbolRef<T extends Symbol> {
        T getTargetSymbol();
    }

    // Resolving generics for Lombokification...
    private interface AJCVarSymbolRef extends AJCSymbolRef<VarSymbol> {}
    private interface AJCMethodSymbolRef extends AJCSymbolRef<MethodSymbol> {}
    private interface AJCClassSymbolRef extends AJCSymbolRef<ClassSymbol> {}
    private interface AJCTypeSymbolRef extends AJCSymbolRef<TypeSymbol> {}

    /** An interface for tree factories */
    public interface Factory {
        AJCClassDecl ClassDef(AJCModifiers mods, Name name, AJCExpressionTree extending,
                              List<AJCExpressionTree> implementing, List<AJCVariableDecl> fields, List<AJCMethodDecl> methods, List<AJCClassDecl> classes);
        AJCMethodDecl MethodDef(AJCModifiers mods,
                               Name name,
                               AJCTypeExpression restype,
                               AJCVariableDecl recvparam,
                               List<AJCVariableDecl> params,
                               List<AJCExpressionTree> thrown,
                               AJCBlock body,
                               AJCExpressionTree defaultValue);
        AJCMethodDecl MethodDef(AJCModifiers mods, Name name, AJCTypeExpression restype, List<AJCVariableDecl> params, List<AJCExpressionTree> thrown, AJCBlock body, AJCExpressionTree defaultValue);
        AJCVariableDecl VarDef(AJCModifiers mods,
                              Name name,
                              AJCTypeExpression vartype,
                              AJCExpressionTree init);
        AJCEmptyExpression EmptyExpression();
        AJCVariableDecl VarDef(VarSymbol v, AJCExpressionTree init);
        AJCSkip Skip();
        AJCBlock Block(long flags, List<AJCStatement> stats);
        AJCDoWhileLoop DoLoop(AJCBlock body, AJCExpressionTree cond);
        AJCWhileLoop WhileLoop(AJCExpressionTree cond, AJCBlock body);
        AJCForLoop ForLoop(List<AJCStatement> init,
                          AJCExpressionTree cond,
                          List<AJCExpressionStatement> step,
                          AJCBlock body);
        AJCLabeledStatement Labelled(Name label, AJCStatement body);
        AJCSwitch Switch(AJCExpressionTree selector, List<AJCCase> cases);
        AJCCase Case(AJCExpressionTree pat, List<AJCStatement> stats);
        AJCSynchronized Synchronized(AJCExpressionTree lock, AJCBlock body);
        AJCTry Try(AJCBlock body, List<AJCCatch> catchers, AJCBlock finalizer);
        AJCCatch Catch(AJCVariableDecl param, AJCBlock body);
        AJCConditional Conditional(AJCExpressionTree cond,
                                  AJCExpressionTree thenpart,
                                  AJCExpressionTree elsepart);
        AJCIf If(AJCExpressionTree cond, AJCBlock thenpart, AJCBlock elsepart);
        AJCExpressionStatement Exec(AJCExpressionTree expr);
        AJCBreak Break(Name label);
        AJCContinue Continue(Name label);
        AJCReturn Return(AJCExpressionTree expr);
        AJCThrow Throw(AJCExpressionTree expr);
        AJCCall Call(AJCSymbolRefTree<MethodSymbol> fn, List<AJCExpressionTree> args);
        AJCNewClass NewClass(AJCSymbolRefTree<ClassSymbol> clazz, List<AJCExpressionTree> args, AJCClassDecl def);
        AJCNewArray NewArray(AJCTypeExpression elemtype, List<AJCExpressionTree> dims, List<AJCExpressionTree> elems);
        AJCAssign Assign(AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs);
        AJCAssignOp Assignop(Tag opcode, AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs);
        AJCUnary Unary(Tag opcode, AJCExpressionTree arg);
        AJCUnaryAsg UnaryAsg(Tag opcode, AJCSymbolRefTree<VarSymbol> arg);
        AJCBinary Binary(Tag opcode, AJCExpressionTree lhs, AJCExpressionTree rhs);
        AJCTypeCast TypeCast(AJCTypeExpression clazz, AJCExpressionTree expr);
        AJCInstanceOf InstanceOf(AJCSymbolRefTree<VarSymbol> expr, AJCSymbolRef<TypeSymbol> clazz);
        AJCArrayAccess ArrayAccess(AJCExpressionTree indexed, AJCExpressionTree index);
        AJCFieldAccess Select(AJCExpressionTree selected, Name selector);
        <T extends Symbol> AJCFieldAccess<T> Select(AJCSymbolRefTree<? extends Symbol> base, T sym);
        <T extends Symbol> AJCIdent<T> Ident(Name idname);
        <T extends Symbol> AJCIdent<T> Ident(T sym);
        AJCLiteral Literal(TypeTag tag, Object value);
        AJCLiteral Literal(Object value);
        AJCPrimitiveTypeTree TypeIdent(TypeTag typetag);
        AJCArrayTypeTree TypeArray(AJCTypeExpression elemtype);
        AJCObjectTypeTree ObjectType(AJCSymbolRefTree<TypeSymbol> ref);
        AJCTypeUnion TypeUnion(List<AJCTypeExpression> components);
        AJCAnnotation Annotation(AJCTree annotationType, List<AJCExpressionTree> args);
        AJCModifiers Modifiers(long flags, List<AJCAnnotation> annotations);
        AJCModifiers Modifiers(long flags);
        AJCAnnotatedType AnnotatedType(AJCTypeExpression underlyingType);
        AJCErroneous Erroneous(List<? extends AJCTree> errs);
        AJCLetExpr LetExpr(List<AJCVariableDecl> defs, AJCExpressionTree expr);
    }

}
