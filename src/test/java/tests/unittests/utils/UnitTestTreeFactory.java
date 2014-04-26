package tests.unittests.utils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree.Factory;
import joust.utils.tree.NameFactory;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.tree.TreeUtils;
import lombok.Delegate;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.compiler.StaticCompilerUtils.*;

public final class UnitTestTreeFactory implements Factory {
    // TODO: Might need more than just nulls here...
    private final ClassSymbol testClass = new ClassSymbol(0, NameFactory.getName(), symtab.voidType, null);
    private MethodSymbol testMethod = newMethod();

    @Delegate(excludes = Excludes.class)
    public Factory wrappedInstance;

    public UnitTestTreeFactory(Factory realFactory) {
        wrappedInstance = realFactory;
    }

    private interface Excludes {
        AJCUnary Unary(JCTree.Tag opcode, AJCExpressionTree arg, boolean resolveOperator);
        AJCUnaryAsg UnaryAsg(JCTree.Tag opcode, AJCSymbolRefTree<VarSymbol> arg, boolean resolveOperator);
        AJCBinary Binary(JCTree.Tag opcode, AJCExpressionTree lhs, AJCExpressionTree rhs, boolean resolveOperator);
        AJCAssignOp Assignop(JCTree.Tag opcode, AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs, boolean resolveOperator);
        AJCUnary Unary(JCTree.Tag opcode, AJCExpressionTree arg);
        AJCUnaryAsg UnaryAsg(JCTree.Tag opcode, AJCSymbolRefTree<VarSymbol> arg);
        AJCBinary Binary(JCTree.Tag opcode, AJCExpressionTree lhs, AJCExpressionTree rhs);
        AJCAssignOp Assignop(JCTree.Tag opcode, AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs);
        <T extends Symbol> AJCIdent Ident(T sym);
        AJCErroneous Erroneous(List<? extends AJCTree> errs);
    }

    // Override the factory methods for the operator expressions, as operator inference is impossible without a javac
    // environment.

    @Override
    public AJCUnary Unary(JCTree.Tag opcode, AJCExpressionTree arg, boolean resolveOperator) {
        AJCUnary ret = new AJCUnary(javacTreeMaker.Unary(opcode, arg.getDecoratedTree()), arg);

        arg.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCUnaryAsg UnaryAsg(JCTree.Tag opcode, AJCSymbolRefTree<VarSymbol> arg, boolean resolveOperator) {
        if (opcode == JCTree.Tag.PREINC
                || opcode == JCTree.Tag.PREDEC
                || opcode == JCTree.Tag.POSTINC
                || opcode == JCTree.Tag.POSTDEC) {
            AJCUnaryAsg ret =  new AJCUnaryAsg(javacTreeMaker.Unary(opcode, arg.getDecoratedTree()), arg);

            arg.mParentNode = ret;

            return ret;
        }
        return null;
    }

    @Override
    public AJCBinary Binary(JCTree.Tag opcode, AJCExpressionTree lhs, AJCExpressionTree rhs, boolean resolveOperator) {
        AJCBinary ret = new AJCBinary(javacTreeMaker.Binary(opcode, lhs.getDecoratedTree(), rhs.getDecoratedTree()),
                lhs, rhs);

        lhs.mParentNode = ret;
        rhs.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCAssignOp Assignop(JCTree.Tag opcode, AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs, boolean resolveOperator) {
        AJCAssignOp ret = new AJCAssignOp(javacTreeMaker.Assignop(opcode, lhs.getDecoratedTree(), rhs.getDecoratedTree()), lhs, rhs);

        lhs.mParentNode = ret;
        rhs.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCAssignOp Assignop(JCTree.Tag opcode, AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs) {
        return Assignop(opcode, lhs, rhs, true);
    }

    @Override
    public AJCUnary Unary(JCTree.Tag opcode, AJCExpressionTree arg) {
        return Unary(opcode, arg, true);
    }

    @Override
    public AJCUnaryAsg UnaryAsg(JCTree.Tag opcode, AJCSymbolRefTree<VarSymbol> arg) {
        return UnaryAsg(opcode, arg, true);
    }

    @Override
    public AJCBinary Binary(JCTree.Tag opcode, AJCExpressionTree lhs, AJCExpressionTree rhs) {
        return Binary(opcode, lhs, rhs, true);
    }


    public AJCVariableDecl VarDef(AJCModifiers mods, Name name, AJCPrimitiveTypeTree vartype, AJCExpressionTree init) {
        AJCVariableDecl tree = wrappedInstance.VarDef(mods, name, vartype, init);
        tree.getDecoratedTree().sym = getFreshVarSymbol(name, vartype);
        tree.setType(tree.getDecoratedTree().sym.type);

        return tree;
    }

    public AJCVariableDecl VarDef(AJCModifiers mods, Name name, AJCArrayTypeTree vartype, AJCExpressionTree init) {
        AJCVariableDecl tree = wrappedInstance.VarDef(mods, name, vartype, init);
        tree.getDecoratedTree().sym = getFreshVarSymbol(name, vartype);
        tree.setType(tree.getDecoratedTree().sym.type);

        return tree;
    }

    public <T extends Symbol> AJCFieldAccess Select(AJCSymbolRefTree<T> selected, Name selector) {
        return null;
    }

    public <T extends Symbol> AJCFieldAccess Select(AJCSymbolRefTree<T> base, Symbol sym) {
        return null;
    }

    @Override
    public <T extends Symbol> AJCIdent Ident(T sym) {
        AJCIdent ident = wrappedInstance.Ident(sym);
        ident.getDecoratedTree().sym = sym;
        return ident;
    }

    @Override
    public AJCErroneous Erroneous(List<? extends AJCTree> errs) {
        return null;
    }

    private VarSymbol getFreshVarSymbol(Name name, AJCPrimitiveTypeTree type) {
        return new VarSymbol(0, name, TreeUtils.typeTreeToType(type), testMethod);
    }
    private VarSymbol getFreshVarSymbol(Name name, AJCArrayTypeTree type) {
        return new VarSymbol(0, name, TreeUtils.typeTreeToType(type), testMethod);
    }

    // Shorthand functions for primitive types...
    public AJCPrimitiveTypeTree Int() {
        return TypeIdent(TypeTag.INT);
    }

    public AJCPrimitiveTypeTree Boolean() {
        return TypeIdent(TypeTag.BOOLEAN);
    }

    public AJCPrimitiveTypeTree Double() {
        return TypeIdent(TypeTag.DOUBLE);
    }

    public AJCPrimitiveTypeTree Float() {
        return TypeIdent(TypeTag.FLOAT);
    }

    public AJCPrimitiveTypeTree Short() {
        return TypeIdent(TypeTag.SHORT);
    }

    public AJCPrimitiveTypeTree Byte() {
        return TypeIdent(TypeTag.BYTE);
    }

    public AJCPrimitiveTypeTree Long() {
        return TypeIdent(TypeTag.LONG);
    }

    public AJCArrayTypeTree Array(AJCTypeExpression elemType) {
        return wrappedInstance.TypeArray(elemType);
    }

    public AJCVariableDecl local(Name name, AJCPrimitiveTypeTree vartype, AJCExpressionTree init) {
        return VarDef(Modifiers(0), name, vartype, init);
    }

    public AJCVariableDecl local(Name name, AJCPrimitiveTypeTree vartype) {
        return VarDef(Modifiers(0), name, vartype, EmptyExpression());
    }

    public AJCVariableDecl local(Name name, AJCArrayTypeTree vartype, AJCExpressionTree init) {
        return VarDef(Modifiers(0), name, vartype, init);
    }

    public AJCVariableDecl local(Name name, AJCArrayTypeTree vartype) {
        return VarDef(Modifiers(0), name, vartype, EmptyExpression());
    }

    /**
     * Build a block from the given trees, wrapping them in Execs as required.
     */
    public AJCBlock Block(AJCTree... trees) {
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
    public MethodSymbol newMethod() {
        testMethod = new MethodSymbol(0, NameFactory.getName(), symtab.voidType, testClass);
        return testMethod;
    }

    /**
     * Complete the in-progress test method with the given block and create a new method symbol for future constructions.
     */
    public AJCMethodDecl MethodFromBlock(AJCBlock block) {
        AJCMethodDecl decl = wrappedInstance.MethodDef(Modifiers(0), NameFactory.getName(), TypeIdent(TypeTag.VOID), null, List.<AJCVariableDecl>nil(), List.<AJCExpressionTree>nil(), block, null);
        decl.getDecoratedTree().sym = testMethod;

        // Create a new virtual method, as the current one is finished.
        newMethod();

        return decl;
    }

    /**
     * Get a method call to a nonexistent method that uses the given symbol.
     */
    @SuppressWarnings("unchecked")
    public AJCCall callFor(VarSymbol sym) {
        MethodSymbol LIES = new MethodSymbol(0, NameFactory.getName(), symtab.voidType, testClass);

        return Call(Ident(LIES), List.<AJCExpressionTree>of(Ident(sym)));
    }

    public MethodSymbol virtualMethod(Type returnType) {
        return new MethodSymbol(0, NameFactory.getName(), returnType, testClass);
    }
    public MethodSymbol virtualMethod() {
        return virtualMethod(symtab.voidType);
    }

    public AJCLiteral l(Object o) {
        // Workaround for bug 9008072 in OpenJDK...
        if (o instanceof Character) {
            // Yes. That's actually how javac does the conversion.
            return wrappedInstance.Literal(TypeTag.CHAR, (int) o.toString().charAt(0));
        }
        return wrappedInstance.Literal(o);
    }

    /*
     * Binary operators...
     */
    public AJCBinary plus(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.PLUS, lValue, rValue);
    }

    public AJCBinary minus(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.MINUS, lValue, rValue);
    }

    public AJCBinary mul(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.MUL, lValue, rValue);
    }

    public AJCBinary div(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.DIV, lValue, rValue);
    }

    public AJCBinary mod(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.MOD, lValue, rValue);
    }

    public AJCBinary and(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.AND, lValue, rValue);
    }

    public AJCBinary or(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.OR, lValue, rValue);
    }

    public AJCBinary bitAnd(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.BITAND, lValue, rValue);
    }

    public AJCBinary bitOr(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.BITOR, lValue, rValue);
    }

    public AJCBinary bitXor(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.BITXOR, lValue, rValue);
    }

    public AJCBinary lShift(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.SL, lValue, rValue);
    }

    public AJCBinary rShift(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.SR, lValue, rValue);
    }

    public AJCBinary urShift(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.USR, lValue, rValue);
    }

    public AJCBinary eq(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.EQ, lValue, rValue);
    }

    public AJCBinary neq(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.NE, lValue, rValue);
    }

    public AJCBinary lt(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.LT, lValue, rValue);
    }

    public AJCBinary gt(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.GT, lValue, rValue);
    }

    public AJCBinary le(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.LE, lValue, rValue);
    }

    public AJCBinary ge(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return Binary(JCTree.Tag.GE, lValue, rValue);
    }

    /*
     * Unary operators...
     */

    public AJCUnary pos(AJCExpressionTree lValue) {
        return Unary(JCTree.Tag.POS, lValue);
    }

    public AJCUnary neg(AJCExpressionTree lValue) {
        return Unary(JCTree.Tag.NEG, lValue);
    }

    public AJCUnary not(AJCExpressionTree lValue) {
        return Unary(JCTree.Tag.NOT, lValue);
    }

    public AJCUnary comp(AJCExpressionTree lValue) {
        return Unary(JCTree.Tag.COMPL, lValue);
    }


    public AJCUnaryAsg preInc(AJCSymbolRefTree<VarSymbol> lValue) {
        return UnaryAsg(JCTree.Tag.PREINC, lValue);
    }

    public AJCUnaryAsg postInc(AJCSymbolRefTree<VarSymbol> lValue) {
        return UnaryAsg(JCTree.Tag.POSTINC, lValue);
    }

    public AJCUnaryAsg preDec(AJCSymbolRefTree<VarSymbol> lValue) {
        return UnaryAsg(JCTree.Tag.PREDEC, lValue);
    }

    public AJCUnaryAsg postDec(AJCSymbolRefTree<VarSymbol> lValue) {
        return UnaryAsg(JCTree.Tag.POSTDEC, lValue);
    }
}
