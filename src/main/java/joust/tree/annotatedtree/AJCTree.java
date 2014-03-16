package joust.tree.annotatedtree;

import com.sun.source.tree.*;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableExpression;
import joust.optimisers.visitors.sideeffects.Effects;
import joust.treeinfo.EffectSet;
import joust.utils.JavacListUtils;
import joust.utils.LogUtils;
import lombok.Delegate;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import java.lang.reflect.Field;
import java.util.Set;

import static com.sun.tools.javac.code.Symbol.*;
import static com.sun.tools.javac.tree.JCTree.*;

/**
 * A representation of the Java AST. Decorates the nodes in javac's existing AST representation with the extra
 * information required, and slightly tweaks the API to make it suck less (No more fields of type JCTree!)
 */
@Log4j2
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
    protected AJCTree mParentNode;

    protected AJCTree(JCTree tree) {
        decoratedTree = tree;
    }

    /**
     * Replace this node with the given node, if possible.
     */
    public void swapFor(AJCTree replacement) {
        if (mParentNode == null) {
            LogUtils.raiseCompilerError("Unable to swap " + this + " for " + replacement + " - parent was null.");
            return;
        }

        // TODO: This is EEEVIL.

        // Determine which of the fields on the parent are occupied by this node and make the appropriate swap.
        // It is required that no object is inserted more than once into a particular parent.
        Class<?> pClass = mParentNode.getClass();
        Field[] fields = pClass.getFields();

        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            try {
                // For non-list fields, just swap in parent.
                if(!"com.sun.tools.javac.util.List".equals(pClass.getCanonicalName())) {
                    if (fields[i].get(mParentNode) == this) {
                        // Target found!
                        fields[i].set(mParentNode, replacement);
                        replacement.mParentNode = mParentNode;

                        // Push this change onto the JCTree, as well...
                        Class<? extends JCTree> underlyingClass = mParentNode.decoratedTree.getClass();
                        Field targetField = underlyingClass.getDeclaredField(fields[i].getName());
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
                    replacement.mParentNode = mParentNode;
                    fields[i].set(mParentNode, theList);
                }
            } catch (IllegalAccessException e) {
                // Ostensibly can never happen, because setAccessible is called...
                log.error("IllegalAccessException accessing field {} on {}", fields[i], pClass.getCanonicalName(), e);
                LogUtils.raiseCompilerError("IllegalAccessException accessing field " + fields[i] + " on " + pClass.getCanonicalName());
            } catch (NoSuchFieldException e) {
                log.error("NoSuchFieldException accessing field {} on {}", fields[i], pClass.getCanonicalName(), e);
                LogUtils.raiseCompilerError("NoSuchFieldException accessing field " + fields[i] + " on " + pClass.getCanonicalName());
            }
        }

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

    public static class AJCImport extends AJCTree implements ImportTree {
        @Delegate @Getter private final JCImport decoratedTree;

        // The imported class(es).
        public AJCTree qualid;

        protected AJCImport(JCImport tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCImport(JCImport tree, AJCTree id) {
            this(tree);
            qualid = id;
        }
    }

    /**
     * Base class for nodes that can have side effects - avoids allocating effect sets for things like type identifiers.
     */
    public abstract static class AJCEffectAnnotatedTree extends AJCTree {
        public Effects effects = new Effects(EffectSet.ALL_EFFECTS);

        // The results of LVA at this node.
        public Set<VarSymbol> liveVariables;

        // The results of available expression analysis at this node.
        public Set<PotentiallyAvailableExpression> avail;

        protected AJCEffectAnnotatedTree(JCTree tree) {
            super(tree);
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
            } else if (mParentNode instanceof AJCExpression) {
                return ((AJCExpression) mParentNode).getEnclosingBlock();
            }

            LogUtils.raiseCompilerError("Failed to find enclosing block for statement: " + this);
            return null;
        }

        // Since a statement is always in a block, we can provide a specialised swap function and avoid the evilness.
        @Override
        public void swapFor(AJCTree replacement) {
            LogUtils.raiseCompilerError("Attempt to swap an AJCStatement for a non-statement!");
            throw new UnsupportedOperationException("Attempt to swap an AJCStatement for a non-statement!");
        }

        public void swapFor(AJCStatement replacement) {
            // Swap in the enclosing block. Simplez!
            enclosingBlock.swap(this, replacement);
        }

        protected AJCStatement(JCStatement tree) {
            super(tree);
            decoratedTree = tree;
        }

        public boolean isEmptyStatement() {
            return false;
        }
    }

    public abstract static class AJCExpression extends AJCEffectAnnotatedTree implements ExpressionTree {
        @Delegate @Getter private final JCExpression decoratedTree;

        /**
         * Get the statement in which this expression resides.
         */
        public AJCStatement getEnclosingStatement() {
            AJCTree parent = mParentNode;
            while (!(parent instanceof AJCStatement) && parent.mParentNode != null) {
                parent = parent.mParentNode;
            }

            if (!(parent instanceof AJCStatement)) {
                LogUtils.raiseCompilerError("Unable to find enclosing statement for: " + this);
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

        protected AJCExpression(JCExpression tree) {
            super(tree);
            decoratedTree = tree;
        }

        public boolean isEmptyExpression() {
            return false;
        }
    }

    public static class AJCConditional extends AJCExpression implements ConditionalExpressionTree {
        @Delegate @Getter private final JCConditional decoratedTree;

        public AJCExpression cond;
        public AJCExpression truepart;
        public AJCExpression falsepart;

        protected AJCConditional(JCConditional tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCConditional(JCConditional tree, AJCExpression c, AJCExpression tPart, AJCExpression fPart) {
            this(tree);
            cond = c;
            truepart = tPart;
            falsepart = fPart;
        }
    }

    public static class AJCCall extends AJCSymbolRefTree<MethodSymbol> implements MethodInvocationTree {
        @Delegate @Getter private final JCMethodInvocation decoratedTree;

        public List<AJCExpression> typeargs;
        @Delegate
        public AJCSymbolRefTree<MethodSymbol> meth;
        public List<AJCExpression> args;

        protected AJCCall(JCMethodInvocation tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCCall(JCMethodInvocation tree, List<AJCExpression> tArgs, AJCSymbolRefTree<MethodSymbol> m, List<AJCExpression> arg) {
            this(tree);
            typeargs = tArgs;
            meth = m;
            args = arg;
        }
    }

    public static class AJCNewClass extends AJCSymbolRefTree<MethodSymbol> implements NewClassTree {
        @Delegate @Getter private final JCNewClass decoratedTree;

        public AJCExpression encl;
        public List<AJCExpression> typeargs;
        public AJCExpression clazz;
        public List<AJCExpression> args;
        public AJCClassDecl def;

        protected AJCNewClass(JCNewClass tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCNewClass(JCNewClass tree, AJCExpression enc, List<AJCExpression> tArgs, AJCExpression clasz,
                             List<AJCExpression> arg, AJCClassDecl defn) {
            this(tree);
            encl = enc;
            typeargs = (tArgs == null) ? List.<AJCExpression>nil() : typeargs;
            clazz = clasz;
            args = arg;
            def = defn;

        }

        @Override
        public MethodSymbol getTargetSymbol() {
            return (MethodSymbol) decoratedTree.constructor;
        }
    }

    public abstract static class AJCFunctionalExpression extends AJCExpression {
        @Delegate @Getter private final JCFunctionalExpression decoratedTree;

        protected AJCFunctionalExpression(JCFunctionalExpression tree) {
            super(tree);
            decoratedTree = tree;
        }
    }

    public static class AJCLambda extends AJCFunctionalExpression implements LambdaExpressionTree {
        @Delegate @Getter private final JCLambda decoratedTree;

        public List<AJCVariableDecl> params;
        public AJCTree body;

        protected AJCLambda(JCLambda tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCLambda(JCLambda tree, List<AJCVariableDecl> param, AJCTree bod) {
            this(tree);
            params = param;
            body = bod;
        }
    }

    public static class AJCClassDecl extends AJCTree implements ClassTree {
        @Delegate @Getter private final JCClassDecl decoratedTree;

        public AJCModifiers mods;
        /** formal class parameters */
        public List<AJCTypeParameter> typarams;
        /** the classes this class extends */
        public AJCExpression extending;
        /** the interfaces implemented by this class */
        public List<AJCExpression> implementing;

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

        protected AJCClassDecl(JCClassDecl tree, AJCModifiers mods, List<AJCTypeParameter> typarams,
                              AJCExpression extending, List<AJCExpression> implementing, List<AJCVariableDecl> fields,
                              List<AJCMethodDecl> methods, List<AJCClassDecl> classes) {
            this(tree);
            this.mods = mods;
            this.typarams = typarams;
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
        public AJCExpression restype;
        /** type parameters */
        public List<AJCTypeParameter> typarams;
        /** receiver parameter */
        public AJCVariableDecl recvparam;
        /** value parameters */
        public List<AJCVariableDecl> params;
        /** exceptions thrown by this method */
        public List<AJCExpression> thrown;
        /** statements in the method */
        public AJCBlock body;
        /** default value, for annotation types */
        public AJCExpression defaultValue;

        // All symbols ever live within the body of this method.
        public Set<VarSymbol> everLive;

        protected AJCMethodDecl(JCMethodDecl tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCMethodDecl(JCMethodDecl tree, AJCModifiers mods, AJCExpression restype,  List<AJCTypeParameter> typarams,
                               AJCVariableDecl recvparam, List<AJCVariableDecl> params, List<AJCExpression> thrown, AJCBlock body,
                               AJCExpression defaultValue)
        {
            this(tree);
            this.mods = mods;
            this.restype = restype;
            this.typarams = typarams;
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
        /** variable name expression */
        public AJCExpression nameexpr;
        /** type of the variable */
        public AJCTypeExpression vartype;
        /** variable's initial value */
        @Getter protected AJCExpression init = new AJCEmptyExpression();

        protected AJCVariableDecl(JCVariableDecl tree) {
            super(tree);
            decoratedTree = tree;
        }


        protected AJCVariableDecl(JCVariableDecl tree, AJCModifiers mods, AJCTypeExpression vartype,
                                 AJCExpression init) {
            this(tree);
            this.mods = mods;
            this.vartype = vartype;
            this.init = init;
        }

        public void setInit(AJCExpression expr) {
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
    public static class AJCEmptyExpression extends AJCExpression {
        protected AJCEmptyExpression() {
            super(null);
        }

        @Override
        public boolean isEmptyExpression() {
            return true;
        }
    }

    public static class AJCSkip extends AJCStatement implements EmptyStatementTree {
        @Delegate @Getter private final JCSkip decoratedTree;

        protected AJCSkip(JCSkip tree) {
            super(tree);
            decoratedTree = tree;
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
            // TODO: Need to update EffectSets to be happy with this change, too...
            // Walking up the tree unioning EffectSets is sufficient, but once you reach the enclosing method you need
            // a dependency graph for those to facilitate dispersal of the change across recursive calls and suchlike.
        }

        /**
         * Add the given statement at the specified index in the block, reflecting the update in underlying decorated block.
         */
        public void insert(int index, AJCStatement statement) {
            stats = JavacListUtils.addAtIndex(stats, index, statement);
            decoratedTree.stats = JavacListUtils.addAtIndex(decoratedTree.stats, index, statement.decoratedTree);
        }

        /**
         * Insert a statement before a given statement.
         */
        public void insertBefore(AJCStatement insert, AJCStatement before) {
            int index = stats.indexOf(before);
            if (index == -1) {
                LogUtils.raiseCompilerError("Unable to insert statement " + insert + " before non-existent statment " + before + " in block:\n" + this);
                return;
            }

            insert(index, insert);
        }

        /**
         * Insert a statement before a given statement.
         */
        public void insertAfter(AJCStatement insert, AJCStatement after) {
            int index = stats.indexOf(after);
            if (index == -1) {
                LogUtils.raiseCompilerError("Unable to insert statement " + insert + " after non-existent statment " + after + " in block:\n" + this);
                return;
            }

            insert(index + 1, insert);
        }

        public int indexOf(AJCStatement statement) {
            return stats.indexOf(statement);
        }

        /**
         * Swap the given statement for the other given statement.
         */
        public void swap(AJCStatement target, AJCStatement replacement) {
            stats = JavacListUtils.replace(stats, target, replacement);
            decoratedTree.stats = JavacListUtils.replace(decoratedTree.stats, target.decoratedTree, replacement.decoratedTree);
        }
    }

    public static class AJCDoWhileLoop extends AJCStatement implements DoWhileLoopTree {
        @Delegate @Getter private final JCDoWhileLoop decoratedTree;

        public AJCBlock body;
        public AJCExpression cond;

        protected AJCDoWhileLoop(JCDoWhileLoop tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCDoWhileLoop(JCDoWhileLoop tree, AJCBlock body, AJCExpression cond) {
            this(tree);
            this.body = body;
            this.cond = cond;
        }
    }

    public static class AJCWhileLoop extends AJCStatement implements WhileLoopTree {
        @Delegate @Getter private final JCWhileLoop decoratedTree;

        public AJCExpression cond;
        public AJCBlock body;

        protected AJCWhileLoop(JCWhileLoop tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCWhileLoop(JCWhileLoop tree, AJCExpression cond, AJCBlock body) {
            this(tree);
            this.body = body;
            this.cond = cond;
        }
    }

    public static class AJCForLoop extends AJCStatement implements ForLoopTree {
        @Delegate @Getter private final JCForLoop decoratedTree;

        public List<AJCStatement> init;
        public AJCExpression cond;
        public List<AJCExpressionStatement> step;
        public AJCBlock body;

        protected AJCForLoop(JCForLoop tree) {
            super(tree);
            decoratedTree = tree;
        }


        protected AJCForLoop(JCForLoop tree, List<AJCStatement> init, AJCExpression cond, List<AJCExpressionStatement> update, AJCBlock body) {
            this(tree);
            this.init = init;
            this.cond = cond;
            this.step = update;
            this.body = body;
        }
    }

    public static class AJCForEachLoop extends AJCStatement implements EnhancedForLoopTree {
        @Delegate @Getter private final JCEnhancedForLoop decoratedTree;

        public AJCVariableDecl var;
        public AJCExpression expr;
        public AJCBlock body;

        protected AJCForEachLoop(JCEnhancedForLoop tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCForEachLoop(JCEnhancedForLoop tree, AJCVariableDecl var, AJCExpression expr, AJCBlock body) {
            this(tree);
            this.var = var;
            this.expr = expr;
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

        public AJCExpression selector;
        public List<AJCCase> cases;

        protected AJCSwitch(JCSwitch tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCSwitch(JCSwitch tree, AJCExpression selector, List<AJCCase> cases) {
            this(tree);
            this.selector = selector;
            this.cases = cases;
        }
    }

    public static class AJCCase extends AJCStatement implements CaseTree {
        @Delegate @Getter private final JCCase decoratedTree;

        // The empty expression here represents the default case.
        public AJCExpression pat = new AJCEmptyExpression();
        public List<AJCStatement> stats;

        protected AJCCase(JCCase tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCCase(JCCase tree, AJCExpression pat, List<AJCStatement> stats) {
            this(tree);
            this.pat = pat;
            this.stats = stats;
        }
    }

    public static class AJCSynchronized extends AJCStatement implements SynchronizedTree {
        @Delegate @Getter private final JCSynchronized decoratedTree;

        public AJCExpression lock;
        public AJCBlock body;

        protected AJCSynchronized(JCSynchronized tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCSynchronized(JCSynchronized tree, AJCExpression lock, AJCBlock body) {
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
        public List<AJCEffectAnnotatedTree> resources;

        protected AJCTry(JCTry tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCTry(JCTry tree, List<AJCEffectAnnotatedTree> resources, AJCBlock body, List<AJCCatch> catchers, AJCBlock finalizer) {
            this(tree);
            this.body = body;
            this.catchers = catchers;
            this.finalizer = finalizer;
            this.resources = resources;
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

        public AJCExpression cond;
        public AJCBlock thenpart;
        public AJCBlock elsepart;

        protected AJCIf(JCIf tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCIf(JCIf tree, AJCExpression cond, AJCBlock thenpart, AJCBlock elsepart) {
            this(tree);
            this.cond = cond;
            this.thenpart = thenpart;
            this.elsepart = elsepart;
        }
    }

    public static class AJCExpressionStatement extends AJCStatement implements ExpressionStatementTree {
        @Delegate @Getter private final JCExpressionStatement decoratedTree;

        public AJCExpression expr;

        protected AJCExpressionStatement(JCExpressionStatement tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCExpressionStatement(JCExpressionStatement tree, AJCExpression expr) {
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

        public AJCExpression expr;

        protected AJCReturn(JCReturn tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCReturn(JCReturn tree, AJCExpression expr) {
            this(tree);
            this.expr = expr;
        }
    }

    public static class AJCThrow extends AJCStatement implements ThrowTree {
        @Delegate @Getter private final JCThrow decoratedTree;

        public AJCExpression expr;

        protected AJCThrow(JCThrow tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCThrow(JCThrow tree, AJCExpression expr) {
            this(tree);
            this.expr = expr;
        }
    }

    public static class AJCAssert extends AJCStatement implements AssertTree {
        @Delegate @Getter private final JCAssert decoratedTree;

        public AJCExpression cond;
        public AJCExpression detail;

        protected AJCAssert(JCAssert tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCAssert(JCAssert tree, AJCExpression cond, AJCExpression detail) {
            this(tree);
            this.cond = cond;
            this.detail = detail;
        }
    }

    public static class AJCNewArray extends AJCExpression implements NewArrayTree {
        @Delegate @Getter private final JCNewArray decoratedTree;

        public AJCExpression elemtype;
        public List<AJCExpression> dims;
        // type annotations on inner-most component
        public List<AJCAnnotation> annotations = List.nil();
        // type annotations on dimensions
        public List<List<AJCAnnotation>> dimAnnotations = List.nil();
        public List<AJCExpression> elems;

        protected AJCNewArray(JCNewArray tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCNewArray(JCNewArray tree, AJCExpression elemtype, List<AJCExpression> dims, List<AJCExpression> elems) {
            this(tree);
            this.elemtype = elemtype;
            this.dims = dims;
            this.elems = elems;
        }
    }

    public static class AJCAssign extends AJCExpression implements AssignmentTree, AJCSymbolRef<VarSymbol> {
        @Delegate @Getter private final JCAssign decoratedTree;

        public AJCSymbolRefTree<VarSymbol> lhs;
        public AJCExpression rhs;

        protected AJCAssign(JCAssign tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCAssign(JCAssign tree, AJCSymbolRefTree<VarSymbol> lhs, AJCExpression rhs) {
            this(tree);
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public VarSymbol getTargetSymbol() {
            return lhs.getTargetSymbol();
        }
    }

    public abstract static class AJCOperatorExpression extends AJCExpression {
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
                LogUtils.raiseCompilerError("Unknown operator expression type: " + tree.getClass().getCanonicalName());
                return;
            }
        }

        public abstract Symbol getOperator();
    }

    public static class AJCAssignOp extends AJCOperatorExpression implements CompoundAssignmentTree, AJCSymbolRef<VarSymbol> {
        @Delegate @Getter private final JCAssignOp decoratedTree;

        @Delegate
        public AJCSymbolRefTree<VarSymbol> lhs;
        public AJCExpression rhs;

        protected AJCAssignOp(JCAssignOp tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCAssignOp(JCAssignOp tree, AJCSymbolRefTree<VarSymbol> lhs, AJCExpression rhs) {
            this(tree);
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    public static class AJCUnary extends AJCOperatorExpression implements UnaryTree {
        @Delegate @Getter private final JCUnary decoratedTree;

        public AJCExpression arg;

        protected AJCUnary(JCUnary tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCUnary(JCUnary tree, AJCExpression arg) {
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

        public AJCExpression lhs;
        public AJCExpression rhs;

        protected AJCBinary(JCBinary tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCBinary(JCBinary tree, AJCExpression lhs, AJCExpression rhs) {
            this(tree);
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    public static class AJCTypeCast extends AJCExpression implements TypeCastTree {
        @Delegate @Getter private final JCTypeCast decoratedTree;

        public AJCTree clazz;
        public AJCExpression expr;

        protected AJCTypeCast(JCTypeCast tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCTypeCast(JCTypeCast tree, AJCTree clazz, AJCExpression expr) {
            this(tree);
            this.clazz = clazz;
            this.expr = expr;
        }
    }

    public static class AJCInstanceOf extends AJCExpression implements InstanceOfTree, AJCSymbolRef<VarSymbol> {
        @Delegate @Getter private final JCInstanceOf decoratedTree;

        @Delegate
        public AJCSymbolRefTree<VarSymbol> expr;
        public AJCTree clazz;

        protected AJCInstanceOf(JCInstanceOf tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCInstanceOf(JCInstanceOf tree, AJCSymbolRefTree<VarSymbol> expr, AJCTree clazz) {
            this(tree);
            this.expr = expr;
            this.clazz = clazz;
        }
    }

    public static class AJCArrayAccess extends AJCExpression implements ArrayAccessTree, AJCSymbolRef<VarSymbol>  {
        @Delegate @Getter private final JCArrayAccess decoratedTree;

        @Delegate
        public AJCSymbolRefTree<VarSymbol> indexed;
        public AJCExpression index;

        protected AJCArrayAccess(JCArrayAccess tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCArrayAccess(JCArrayAccess tree, AJCSymbolRefTree<VarSymbol> indexed, AJCExpression index) {
            this(tree);
            this.indexed = indexed;
            this.index = index;
        }
    }

    public static class AJCFieldAccess<T extends Symbol> extends AJCSymbolRefTree<T> implements MemberSelectTree {
        @Delegate @Getter private final JCFieldAccess decoratedTree;

        /** selected Tree hierarchy */
        public AJCSymbolRefTree selected;

        protected AJCFieldAccess(JCFieldAccess tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCFieldAccess(JCFieldAccess tree, AJCSymbolRefTree selected) {
            this(tree);
            this.selected = selected;
        }

        @SuppressWarnings("unchecked")
        public T getSym() {
            return (T) decoratedTree.sym;
        }

        /**
         * The target symbol is that of the one at the end of the chain of references...
         */
        @SuppressWarnings("unchecked")
        @Override
        public T getTargetSymbol() {
            return (T) selected.getTargetSymbol();
        }
    }

    // TODO: Implements AJCSymbolRefTree?
    public static class AJCMemberReference extends AJCFunctionalExpression implements MemberReferenceTree {
        @Delegate @Getter private final JCMemberReference decoratedTree;

        public AJCExpression expr;
        public List<AJCExpression> typeargs;

        protected AJCMemberReference(JCMemberReference tree) {
            super(tree);
            decoratedTree = tree;
        }

        public Symbol getSym() {
            return decoratedTree.sym;
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

    public static class AJCLiteral extends AJCExpression implements LiteralTree {
        @Delegate @Getter private final JCLiteral decoratedTree;

        protected AJCLiteral(JCLiteral tree) {
            super(tree);
            decoratedTree = tree;
        }
    }


    public abstract static class AJCTypeTree extends AJCTree {
        protected AJCTypeTree(JCTree tree) {
            super(tree);
        }
    }

    /**
     * Base class of type expressions - may not hold an effect annotation.
     */
    public abstract static class AJCTypeExpression extends AJCTypeTree implements ExpressionTree {
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
    }

    public static class AJCArrayTypeTree extends AJCTypeExpression implements ArrayTypeTree {
        @Delegate @Getter private final JCArrayTypeTree decoratedTree;

        public AJCExpression elemtype;

        protected AJCArrayTypeTree(JCArrayTypeTree tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCArrayTypeTree(JCArrayTypeTree tree, AJCExpression elemtype) {
            this(tree);
            this.elemtype = elemtype;
        }
    }

    public static class AJCTypeApply extends AJCTypeExpression implements ParameterizedTypeTree {
        @Delegate @Getter private final JCTypeApply decoratedTree;

        public AJCExpression clazz;
        public List<AJCExpression> arguments;

        protected AJCTypeApply(JCTypeApply tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCTypeApply(JCTypeApply tree, AJCExpression clazz, List<AJCExpression> arguments) {
            this(tree);
            this.clazz = clazz;
            this.arguments = arguments;
        }
    }

    public static class AJCTypeUnion extends AJCTypeExpression implements UnionTypeTree {
        @Delegate @Getter private final JCTypeUnion decoratedTree;

        public List<AJCExpression> alternatives;

        protected AJCTypeUnion(JCTypeUnion tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCTypeUnion(JCTypeUnion tree, List<AJCExpression> components) {
            this(tree);
            this.alternatives = components;
        }
    }

    public static class AJCTypeIntersection extends AJCTypeExpression implements IntersectionTypeTree {
        @Delegate @Getter private final JCTypeIntersection decoratedTree;

        public List<AJCExpression> bounds;

        protected AJCTypeIntersection(JCTypeIntersection tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCTypeIntersection(JCTypeIntersection tree, List<AJCExpression> bounds) {
            this(tree);
            this.bounds = bounds;
        }
    }

    public static class AJCTypeParameter extends AJCTypeTree implements TypeParameterTree {
        @Delegate @Getter private final JCTypeParameter decoratedTree;

        /** bounds */
        public List<AJCExpression> bounds;
        /** type annotations on type parameter */
        public List<AJCAnnotation> annotations;

        protected AJCTypeParameter(JCTypeParameter tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCTypeParameter(JCTypeParameter tree, List<AJCExpression> bounds, List<AJCAnnotation> annotations) {
            this(tree);
            this.bounds = bounds;
            this.annotations = annotations;
        }
    }

    public static class AJCWildcard extends AJCTypeExpression implements WildcardTree {
        @Delegate @Getter private final JCWildcard decoratedTree;

        public AJCTree inner;
        public AJCTypeBoundKind kind;

        protected AJCWildcard(JCWildcard tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCWildcard(JCWildcard tree, AJCTypeBoundKind kind, AJCTree inner) {
            this(tree);
            this.kind = kind;
            this.inner = inner;
        }
    }

    public static class AJCTypeBoundKind extends AJCTypeTree {
        @Delegate @Getter private final TypeBoundKind decoratedTree;

        protected AJCTypeBoundKind(TypeBoundKind tree) {
            super(tree);
            decoratedTree = tree;
        }
    }

    public static class AJCAnnotation extends AJCExpression implements AnnotationTree {
        @Delegate @Getter private final JCAnnotation decoratedTree;

        public AJCTree annotationType;
        public List<AJCExpression> args;

        protected AJCAnnotation(JCAnnotation tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCAnnotation(JCAnnotation tree, AJCTree annotationType, List<AJCExpression> args) {
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


    public static class AJCAnnotatedType extends AJCTypeExpression implements AnnotatedTypeTree {
        @Delegate @Getter private final JCAnnotatedType decoratedTree;

        public List<AJCAnnotation> annotations;
        public AJCExpression underlyingType;

        protected AJCAnnotatedType(JCAnnotatedType tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCAnnotatedType(JCAnnotatedType tree, List<AJCAnnotation> annotations, AJCExpression underlyingType) {
            this(tree);
            this.annotations = annotations;
            this.underlyingType = underlyingType;
        }
    }

    public static class AJCErroneous extends AJCExpression {
        @Delegate @Getter private final JCErroneous decoratedTree;

        protected AJCErroneous(JCErroneous tree) {
            super(tree);
            decoratedTree = tree;
        }
    }

    public static class AJCLetExpr extends AJCExpression {
        @Delegate @Getter private final LetExpr decoratedTree;

        public List<AJCVariableDecl> defs;
        public AJCExpression expr;

        protected AJCLetExpr(LetExpr tree) {
            super(tree);
            decoratedTree = tree;
        }

        protected AJCLetExpr(LetExpr tree, List<AJCVariableDecl> defs, AJCExpression expr) {
            this(tree);
            this.defs = defs;
            this.expr = expr;
        }
    }

    /**
     * An abstract class for nodes that represent references to symbols - Idents and FieldAccesses, mostly.
     */
    public abstract static class AJCSymbolRefTree<T extends Symbol> extends AJCExpression implements AJCSymbolRef<T> {
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

    /** An interface for tree factories */
    public interface Factory {
        AJCImport Import(AJCTree qualid, boolean staticImport);
        AJCClassDecl ClassDef(AJCModifiers mods, Name name, List<AJCTypeParameter> typarams, AJCExpression extending,
                              List<AJCExpression> implementing, List<AJCVariableDecl> fields, List<AJCMethodDecl> methods, List<AJCClassDecl> classes);
        AJCMethodDecl MethodDef(AJCModifiers mods,
                               Name name,
                               AJCExpression restype,
                               List<AJCTypeParameter> typarams,
                               AJCVariableDecl recvparam,
                               List<AJCVariableDecl> params,
                               List<AJCExpression> thrown,
                               AJCBlock body,
                               AJCExpression defaultValue);
        AJCMethodDecl MethodDef(AJCModifiers mods, Name name, AJCExpression restype, List<AJCTypeParameter> typarams, List<AJCVariableDecl> params, List<AJCExpression> thrown, AJCBlock body, AJCExpression defaultValue);
        AJCVariableDecl VarDef(AJCModifiers mods,
                              Name name,
                              AJCTypeExpression vartype,
                              AJCExpression init);
        AJCEmptyExpression EmptyExpression();
        AJCSkip Skip();
        AJCBlock Block(long flags, List<AJCStatement> stats);
        AJCDoWhileLoop DoLoop(AJCBlock body, AJCExpression cond);
        AJCWhileLoop WhileLoop(AJCExpression cond, AJCBlock body);
        AJCForLoop ForLoop(List<AJCStatement> init,
                          AJCExpression cond,
                          List<AJCExpressionStatement> step,
                          AJCBlock body);
        AJCForEachLoop ForeachLoop(AJCVariableDecl var, AJCExpression expr, AJCBlock body);
        AJCLabeledStatement Labelled(Name label, AJCStatement body);
        AJCSwitch Switch(AJCExpression selector, List<AJCCase> cases);
        AJCCase Case(AJCExpression pat, List<AJCStatement> stats);
        AJCSynchronized Synchronized(AJCExpression lock, AJCBlock body);
        AJCTry Try(AJCBlock body, List<AJCCatch> catchers, AJCBlock finalizer);
        AJCTry Try(List<AJCEffectAnnotatedTree> resources, AJCBlock body, List<AJCCatch> catchers, AJCBlock finalizer);
        AJCCatch Catch(AJCVariableDecl param, AJCBlock body);
        AJCConditional Conditional(AJCExpression cond,
                                  AJCExpression thenpart,
                                  AJCExpression elsepart);
        AJCIf If(AJCExpression cond, AJCBlock thenpart, AJCBlock elsepart);
        AJCExpressionStatement Exec(AJCExpression expr);
        AJCBreak Break(Name label);
        AJCContinue Continue(Name label);
        AJCReturn Return(AJCExpression expr);
        AJCThrow Throw(AJCExpression expr);
        AJCAssert Assert(AJCExpression cond, AJCExpression detail);
        AJCCall Call(List<AJCExpression> typeargs, AJCSymbolRefTree<MethodSymbol> fn, List<AJCExpression> args);
        AJCNewClass NewClass(AJCExpression encl,
                            List<AJCExpression> typeargs,
                            AJCExpression clazz,
                            List<AJCExpression> args,
                            AJCClassDecl def);
        AJCNewArray NewArray(AJCExpression elemtype,
                            List<AJCExpression> dims,
                            List<AJCExpression> elems);
        AJCAssign Assign(AJCSymbolRefTree<VarSymbol> lhs, AJCExpression rhs);
        AJCAssignOp Assignop(Tag opcode, AJCSymbolRefTree<VarSymbol> lhs, AJCExpression rhs);
        AJCUnary Unary(Tag opcode, AJCExpression arg);
        AJCUnaryAsg UnaryAsg(Tag opcode, AJCSymbolRefTree<VarSymbol> arg);
        AJCBinary Binary(Tag opcode, AJCExpression lhs, AJCExpression rhs);
        AJCTypeCast TypeCast(AJCTree clazz, AJCExpression expr);
        AJCInstanceOf InstanceOf(AJCSymbolRefTree<VarSymbol> expr, AJCTree clazz);
        AJCArrayAccess ArrayAccess(AJCExpression indexed, AJCExpression index);
        AJCFieldAccess Select(AJCSymbolRefTree selected, Name selector);
        AJCFieldAccess Select(AJCSymbolRefTree base, Symbol sym);
        AJCIdent Ident(Name idname);
        AJCIdent Ident(Symbol sym);
        AJCLiteral Literal(TypeTag tag, Object value);
        AJCLiteral Literal(Object value);
        AJCPrimitiveTypeTree TypeIdent(TypeTag typetag);
        AJCArrayTypeTree TypeArray(AJCExpression elemtype);
        AJCTypeApply TypeApply(AJCExpression clazz, List<AJCExpression> arguments);
        AJCTypeUnion TypeUnion(List<AJCExpression> components);
        AJCTypeIntersection TypeIntersection(List<AJCExpression> components);
        AJCTypeParameter TypeParameter(Name name, List<AJCExpression> bounds);
        AJCTypeParameter TypeParameter(Name name, List<AJCExpression> bounds, List<AJCAnnotation> annos);
        AJCWildcard Wildcard(AJCTypeBoundKind kind, AJCTree type);
        AJCTypeBoundKind TypeBoundKind(BoundKind kind);
        AJCAnnotation Annotation(AJCTree annotationType, List<AJCExpression> args);
        AJCModifiers Modifiers(long flags, List<AJCAnnotation> annotations);
        AJCModifiers Modifiers(long flags);
        AJCAnnotatedType AnnotatedType(List<AJCAnnotation> annotations, AJCExpression underlyingType);
        AJCErroneous Erroneous(List<? extends AJCTree> errs);
        AJCLetExpr LetExpr(List<AJCVariableDecl> defs, AJCExpression expr);
    }

}
