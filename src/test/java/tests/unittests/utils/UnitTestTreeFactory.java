package tests.unittests.utils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.TreeUtils;

import javax.lang.model.type.TypeKind;

import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.StaticCompilerUtils.symtab;
import static joust.utils.StaticCompilerUtils.treeMaker;
import static com.sun.tools.javac.code.Symbol.*;

public final class UnitTestTreeFactory {
    // TODO: Might need more than just nulls here...
    private static final ClassSymbol testClass = new ClassSymbol(0, NameFactory.getName(), symtab.voidType, null);
    private static MethodSymbol testMethod = newMethod();

    public static AJCVariableDecl VarDef(AJCModifiers mods, Name name, AJCPrimitiveTypeTree vartype, AJCExpressionTree init) {
        AJCVariableDecl tree = treeMaker.VarDef(mods, name, vartype, init);
        tree.getDecoratedTree().sym = getFreshVarSymbol(name, vartype);

        return tree;
    }

    public static AJCVariableDecl VarDef(AJCModifiers mods, Name name, AJCArrayTypeTree vartype, AJCExpressionTree init) {
        AJCVariableDecl tree = treeMaker.VarDef(mods, name, vartype, init);
        tree.getDecoratedTree().sym = getFreshVarSymbol(name, vartype);

        return tree;
    }

    public static AJCEmptyExpression EmptyExpression() {
        return treeMaker.EmptyExpression();
    }

    public static AJCSkip Skip() {
        return treeMaker.Skip();
    }

    public static AJCBlock Block(long flags, List<AJCStatement> stats) {
        return treeMaker.Block(flags, stats);
    }

    public static AJCDoWhileLoop DoLoop(AJCBlock body, AJCExpressionTree cond) {
        return treeMaker.DoLoop(body, cond);
    }

    public static AJCWhileLoop WhileLoop(AJCExpressionTree cond, AJCBlock body) {
        return treeMaker.WhileLoop(cond, body);
    }

    public static AJCForLoop ForLoop(List<AJCStatement> init, AJCExpressionTree cond, List<AJCExpressionStatement> step, AJCBlock body) {
        return treeMaker.ForLoop(init, cond, step, body);
    }

    public static AJCLabeledStatement Labelled(Name label, AJCStatement body) {
        return treeMaker.Labelled(label, body);
    }

    public static AJCSwitch Switch(AJCExpressionTree selector, List<AJCCase> cases) {
        return treeMaker.Switch(selector, cases);
    }

    public static AJCCase Case(AJCExpressionTree pat, List<AJCStatement> stats) {
        return treeMaker.Case(pat, stats);
    }

    public static AJCSynchronized Synchronized(AJCExpressionTree lock, AJCBlock body) {
        return treeMaker.Synchronized(lock, body);
    }

    public static AJCTry Try(AJCBlock body, List<AJCCatch> catchers, AJCBlock finalizer) {
        return treeMaker.Try(body, catchers, finalizer);
    }

    public static AJCCatch Catch(AJCVariableDecl param, AJCBlock body) {
        return treeMaker.Catch(param, body);
    }

    public static AJCConditional Conditional(AJCExpressionTree cond, AJCExpressionTree thenpart, AJCExpressionTree elsepart) {
        return treeMaker.Conditional(cond, thenpart, elsepart);
    }

    public static AJCIf If(AJCExpressionTree cond, AJCBlock thenpart, AJCBlock elsepart) {
        return treeMaker.If(cond, thenpart, elsepart);
    }

    public static AJCExpressionStatement Exec(AJCExpressionTree expr) {
        return treeMaker.Exec(expr);
    }

    public static AJCBreak Break(Name label) {
        return treeMaker.Break(label);
    }

    public static AJCContinue Continue(Name label) {
        return treeMaker.Continue(label);
    }

    public static AJCReturn Return(AJCExpressionTree expr) {
        return treeMaker.Return(expr);
    }

    public static AJCThrow Throw(AJCExpressionTree expr) {
        return treeMaker.Throw(expr);
    }

    public static AJCCall Call(AJCSymbolRefTree<Symbol.MethodSymbol> fn, List<AJCExpressionTree> args) {
        return treeMaker.Call(fn, args);
    }

    public static AJCNewClass NewClass(AJCSymbolRefTree<Symbol.ClassSymbol> clazz, List<AJCExpressionTree> args, AJCClassDecl def) {
        return treeMaker.NewClass(clazz, args, def);
    }

    public static AJCNewArray NewArray(AJCTypeExpression elemtype, List<AJCExpressionTree> dims, List<AJCExpressionTree> elems) {
        return treeMaker.NewArray(elemtype, dims, elems);
    }

    public static AJCAssign Assign(AJCSymbolRefTree<Symbol.VarSymbol> lhs, AJCExpressionTree rhs) {
        return treeMaker.Assign(lhs, rhs);
    }

    public static AJCAssignOp Assignop(JCTree.Tag opcode, AJCSymbolRefTree<Symbol.VarSymbol> lhs, AJCExpressionTree rhs) {
        return treeMaker.Assignop(opcode, lhs, rhs);
    }

    public static AJCUnary Unary(JCTree.Tag opcode, AJCExpressionTree arg) {
        return treeMaker.Unary(opcode, arg);
    }

    public static AJCUnaryAsg UnaryAsg(JCTree.Tag opcode, AJCSymbolRefTree<Symbol.VarSymbol> arg) {
        return treeMaker.UnaryAsg(opcode, arg);
    }

    public static AJCBinary Binary(JCTree.Tag opcode, AJCExpressionTree lhs, AJCExpressionTree rhs) {
        return treeMaker.Binary(opcode, lhs, rhs);
    }

    public static AJCTypeCast TypeCast(AJCTypeExpression clazz, AJCExpressionTree expr) {
        return treeMaker.TypeCast(clazz, expr);
    }

    public static AJCInstanceOf InstanceOf(AJCSymbolRefTree<VarSymbol> expr, AJCSymbolRefTree<TypeSymbol> clazz) {
        return treeMaker.InstanceOf(expr, clazz);
    }

    public static AJCArrayAccess ArrayAccess(AJCExpressionTree indexed, AJCExpressionTree index) {
        return treeMaker.ArrayAccess(indexed, index);
    }

    public static <T extends Symbol> AJCFieldAccess Select(AJCSymbolRefTree<T> selected, Name selector) {
        return null;
    }

    public static <T extends Symbol> AJCFieldAccess Select(AJCSymbolRefTree<T> base, Symbol sym) {
        return null;
    }

    public static AJCIdent Ident(Symbol sym) {
        AJCIdent ident = treeMaker.Ident(sym);
        ident.getDecoratedTree().sym = sym;
        return ident;
    }

    public static AJCLiteral Literal(TypeTag tag, Object value) {
        return treeMaker.Literal(tag, value);
    }

    public static AJCLiteral Literal(Object value) {
        return treeMaker.Literal(value);
    }

    public static AJCPrimitiveTypeTree TypeIdent(TypeTag typetag) {
        return treeMaker.TypeIdent(typetag);
    }

    public static AJCArrayTypeTree TypeArray(AJCTypeExpression elemtype) {
        return treeMaker.TypeArray(elemtype);
    }

    public static AJCTypeUnion TypeUnion(List<AJCTypeExpression> components) {
        return treeMaker.TypeUnion(components);
    }

    public static AJCAnnotation Annotation(AJCTree annotationType, List<AJCExpressionTree> args) {
        return treeMaker.Annotation(annotationType, args);
    }

    public static AJCModifiers Modifiers(long flags, List<AJCAnnotation> annotations) {
        return treeMaker.Modifiers(flags, annotations);
    }

    public static AJCModifiers Modifiers(long flags) {
        return treeMaker.Modifiers(flags);
    }

    public static AJCAnnotatedType AnnotatedType(AJCTypeExpression underlyingType) {
        return treeMaker.AnnotatedType(underlyingType);
    }

    public static AJCErroneous Erroneous(List<? extends AJCTree> errs) {
        return null;
    }

    public static AJCLetExpr LetExpr(List<AJCVariableDecl> defs, AJCExpressionTree expr) {
        return treeMaker.LetExpr(defs, expr);
    }

    private static VarSymbol getFreshVarSymbol(Name name, AJCPrimitiveTypeTree type) {
        return new VarSymbol(0, name, TreeUtils.typeTreeToType(type), testMethod);
    }
    private static VarSymbol getFreshVarSymbol(Name name, AJCArrayTypeTree type) {
        return new VarSymbol(0, name, TreeUtils.typeTreeToType(type), testMethod);
    }

    // Shorthand functions for primitive types...
    public static AJCPrimitiveTypeTree Int() {
        return TypeIdent(TypeTag.INT);
    }

    public static AJCPrimitiveTypeTree Boolean() {
        return TypeIdent(TypeTag.BOOLEAN);
    }

    public static AJCPrimitiveTypeTree Double() {
        return TypeIdent(TypeTag.DOUBLE);
    }

    public static AJCPrimitiveTypeTree Float() {
        return TypeIdent(TypeTag.FLOAT);
    }

    public static AJCPrimitiveTypeTree Short() {
        return TypeIdent(TypeTag.SHORT);
    }

    public static AJCPrimitiveTypeTree Byte() {
        return TypeIdent(TypeTag.BYTE);
    }

    public static AJCPrimitiveTypeTree Long() {
        return TypeIdent(TypeTag.LONG);
    }

    public static AJCArrayTypeTree Array(AJCTypeExpression elemType) {
        return treeMaker.TypeArray(elemType);
    }

    public static AJCVariableDecl local(Name name, AJCPrimitiveTypeTree vartype, AJCExpressionTree init) {
        return VarDef(Modifiers(0), name, vartype, init);
    }

    public static AJCVariableDecl local(Name name, AJCPrimitiveTypeTree vartype) {
        return VarDef(Modifiers(0), name, vartype, EmptyExpression());
    }

    public static AJCVariableDecl local(Name name, AJCArrayTypeTree vartype, AJCExpressionTree init) {
        return VarDef(Modifiers(0), name, vartype, init);
    }

    public static AJCVariableDecl local(Name name, AJCArrayTypeTree vartype) {
        return VarDef(Modifiers(0), name, vartype, EmptyExpression());
    }

    /**
     * Build a block from the given trees, wrapping them in Execs as required.
     */
    public static AJCBlock Block(AJCTree... trees) {
        for (int i = 0; i < trees.length; i++) {
            if (trees[i] instanceof AJCExpressionTree) {
                trees[i] = Exec((AJCExpressionTree) trees[i]);
            } else if (!(trees[i] instanceof AJCStatement)) {
                throw new IllegalArgumentException("Unexpected tree for blockification: " + trees[i]);
            }
        }

        List<AJCStatement> stats = List.nil();
        for (int i = trees.length-1; i >= 0; i--) {
            stats = stats.prepend((AJCStatement) trees[i]);
        }

        return Block(0, stats);
    }

    /**
     * Called to cause all future nodes to be created "inside" a fresh virtual method for testing.
     */
    public static MethodSymbol newMethod() {
        testMethod = new MethodSymbol(0, NameFactory.getName(), symtab.voidType, testClass);
        return testMethod;
    }

    /**
     * Complete the in-progress test method with the given block and create a new method symbol for future constructions.
     */
    public static AJCMethodDecl MethodFromBlock(AJCBlock block) {
        AJCMethodDecl decl = treeMaker.MethodDef(Modifiers(0), NameFactory.getName(), TypeIdent(TypeTag.VOID), null, List.<AJCVariableDecl>nil(), List.<AJCExpressionTree>nil(), block, null);
        decl.getDecoratedTree().sym = testMethod;

        // Create a new virtual method, as the current one is finished.
        newMethod();

        return decl;
    }

    /**
     * Get a method call to a nonexistent method that uses the given symbol.
     */
    @SuppressWarnings("unchecked")
    public static AJCCall callFor(VarSymbol sym) {
        MethodSymbol LIES = new MethodSymbol(0, NameFactory.getName(), symtab.voidType, testClass);

        return Call(Ident(LIES), List.<AJCExpressionTree>of(Ident(sym)));
    }

    public static MethodSymbol virtualMethod(Type returnType) {
        return new MethodSymbol(0, NameFactory.getName(), returnType, testClass);
    }
    public static MethodSymbol virtualMethod() {
        return virtualMethod(symtab.voidType);
    }
}
