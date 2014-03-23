package tests.unittests.utils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeFactory;
import joust.utils.StaticCompilerUtils;
import lombok.Delegate;

import javax.lang.model.type.TypeKind;

import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.StaticCompilerUtils.symtab;
import static joust.utils.StaticCompilerUtils.treeMaker;

public class UnitTestTreeFactory {
    public static AJCVariableDecl VarDef(AJCModifiers mods, Name name, AJCTypeExpression vartype, AJCExpressionTree init) {
        if (!(vartype instanceof AJCPrimitiveTypeTree)) {
            throw new UnsupportedOperationException("Only primitive types are supported in the mocked-type environment.");
        }

        AJCVariableDecl tree = treeMaker.VarDef(mods, name, vartype, init);
        tree.getDecoratedTree().sym = getFreshVarSymbol(name, (AJCPrimitiveTypeTree) vartype);

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

    public static AJCInstanceOf InstanceOf(AJCSymbolRefTree<Symbol.VarSymbol> expr, AJCSymbolRefTree<Symbol.ClassSymbol> clazz) {
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

    public static AJCAnnotatedType AnnotatedType(AJCExpressionTree underlyingType) {
        return treeMaker.AnnotatedType(underlyingType);
    }

    public static AJCErroneous Erroneous(List<? extends AJCTree> errs) {
        return null;
    }

    public static AJCLetExpr LetExpr(List<AJCVariableDecl> defs, AJCExpressionTree expr) {
        return treeMaker.LetExpr(defs, expr);
    }

    private static Symbol.VarSymbol getFreshVarSymbol(Name name, AJCPrimitiveTypeTree type) {
        return new Symbol.VarSymbol(0, name, typeTreeToType(type), null);
    }

    private static Type typeTreeToType(AJCPrimitiveTypeTree tree) {
        TypeKind kind = tree.getPrimitiveTypeKind();
        switch(kind) {
            case BOOLEAN:
                return symtab.doubleType;
            case BYTE:
                return symtab.byteType;
            case SHORT:
                return symtab.shortType;
            case INT:
                return symtab.intType;
            case LONG:
                return symtab.longType;
            case CHAR:
                return symtab.charType;
            case FLOAT:
                return symtab.floatType;
            case DOUBLE:
                return symtab.doubleType;
            default:
                throw new UnsupportedOperationException("Unknown primitive type kind encountered: " + kind);
        }
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

    public static AJCVariableDecl local(Name name, AJCTypeExpression vartype, AJCExpressionTree init) {
        return VarDef(Modifiers(0), name, vartype, init);
    }

    public static AJCVariableDecl local(Name name, AJCTypeExpression vartype) {
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

    public static AJCMethodDecl MethodFromBlock(AJCBlock block) {
        return treeMaker.MethodDef(Modifiers(0), NameFactory.getName(), TypeIdent(TypeTag.VOID), null, List.<AJCVariableDecl>nil(), List.<AJCExpressionTree>nil(), block, null);
    }
}
