package joust.tree.annotatedtree;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.LogUtils;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.tree.JCTree.*;
import static joust.utils.StaticCompilerUtils.javacTreeMaker;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A factory for creating tree nodes.
 * Each node is created backed by a JCTree node.
 */
public class AJCTreeFactory implements AJCTree.Factory {
    protected static final Context.Key<AJCTreeFactory> AJCTreeMakerKey = new Context.Key<>();

    Types types;

    public static AJCTreeFactory instance(Context context) {
        AJCTreeFactory instance = context.get(AJCTreeMakerKey);
        if (instance == null) {
            instance = new AJCTreeFactory(context);
        }

        return instance;
    }

    private AJCTreeFactory(Context context) {
        types = Types.instance(context);
    }

    @Override
    public AJCClassDecl ClassDef(AJCModifiers mods, Name name, AJCExpressionTree extending,
                                 List<AJCExpressionTree> implementing,  List<AJCVariableDecl> fields, List<AJCMethodDecl> methods, List<AJCClassDecl> classes) {
        AJCClassDecl ret = new AJCClassDecl(javacTreeMaker.ClassDef(mods.<JCModifiers, AJCModifiers>getDecoratedTree(), name, List.<JCTypeParameter>nil(), extending.getDecoratedTree(),
                AJCTree.<JCExpression, AJCExpressionTree>unwrap(implementing), unwrap(fields).prependList(unwrap(methods)).prependList(unwrap(classes))),
                mods, extending, implementing, fields, methods, classes);

        mods.mParentNode = ret;
        extending.mParentNode = ret;
        for (AJCExpressionTree expr : implementing) {
            expr.mParentNode = ret;
        }
        for (AJCVariableDecl field : fields) {
            field.mParentNode = ret;
        }
        for (AJCMethodDecl meth : methods) {
            meth.mParentNode = ret;
        }
        for (AJCClassDecl clazz : classes) {
            clazz.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCMethodDecl MethodDef(AJCModifiers mods, Name name, AJCTypeExpression restype, AJCVariableDecl recvparam, List<AJCVariableDecl> params, List<AJCExpressionTree> thrown, AJCBlock body, AJCExpressionTree defaultValue) {
        AJCMethodDecl ret = new AJCMethodDecl(javacTreeMaker.MethodDef(mods.<JCModifiers, AJCModifiers>getDecoratedTree(), name, restype.getDecoratedTree(), List.<JCTypeParameter>nil(), recvparam.getDecoratedTree(), AJCTree.<JCVariableDecl, AJCVariableDecl>unwrap(params), AJCTree.<JCExpression, AJCExpressionTree>unwrap(thrown), body.getDecoratedTree(), defaultValue.getDecoratedTree()),
                mods, restype, recvparam, params, thrown, body, defaultValue);

        mods.mParentNode = ret;
        restype.mParentNode = ret;
        recvparam.mParentNode = ret;
        for (AJCVariableDecl param : params) {
            param.mParentNode = ret;
        }
        for (AJCExpressionTree thro : thrown) {
            thro.mParentNode = ret;
        }
        body.mParentNode = ret;
        defaultValue.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCMethodDecl MethodDef(AJCModifiers mods, Name name, AJCTypeExpression restype, List<AJCVariableDecl> params, List<AJCExpressionTree> thrown, AJCBlock body, AJCExpressionTree defaultValue) {
        return MethodDef(mods, name, restype, null, params, thrown, body, defaultValue);
    }

    @Override
    public AJCVariableDecl VarDef(AJCModifiers mods, Name name, AJCTypeExpression vartype, AJCExpressionTree init) {
        AJCVariableDecl ret = new AJCVariableDecl(javacTreeMaker.VarDef(mods.getDecoratedTree(), name, vartype.getDecoratedTree(),
                init.getDecoratedTree()),
                mods, vartype, init);

        mods.mParentNode = ret;
        vartype.mParentNode = ret;
        init.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCSkip Skip() {
        return new AJCSkip(javacTreeMaker.Skip());
    }

    @Override
    public AJCEmptyExpression EmptyExpression() {
        return new AJCEmptyExpression();
    }

    @Override
    public AJCBlock Block(long flags, List<AJCStatement> stats) {
        AJCBlock ret = new AJCBlock(javacTreeMaker.Block(flags, AJCTree.<JCStatement, AJCStatement>unwrap(stats)), stats);

        for (AJCStatement stat : stats) {
            stat.enclosingBlock = ret;
            stat.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCDoWhileLoop DoLoop(AJCBlock body, AJCExpressionTree cond) {
        AJCDoWhileLoop ret = new AJCDoWhileLoop(javacTreeMaker.DoLoop(body.getDecoratedTree(), cond.getDecoratedTree()),
                body, cond);
        body.mParentNode = ret;
        cond.mParentNode = ret;
        return ret;
    }

    @Override
    public AJCWhileLoop WhileLoop(AJCExpressionTree cond, AJCBlock body) {
        AJCWhileLoop ret = new AJCWhileLoop(javacTreeMaker.WhileLoop(cond.getDecoratedTree(), body.getDecoratedTree()),
                cond, body);

        cond.mParentNode = ret;
        body.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCForLoop ForLoop(List<AJCStatement> init, AJCExpressionTree cond, List<AJCExpressionStatement> step, AJCBlock body) {
        AJCForLoop ret = new AJCForLoop(javacTreeMaker.ForLoop(AJCTree.<JCStatement, AJCStatement>unwrap(init), cond.getDecoratedTree(), AJCTree.<JCExpressionStatement, AJCExpressionStatement>unwrap(step), body.getDecoratedTree()),
                init, cond, step, body);

        for (AJCStatement stat : init) {
            stat.mParentNode = ret;
        }
        cond.mParentNode = ret;
        for (AJCStatement stat : step) {
            stat.mParentNode = ret;
        }
        body.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCLabeledStatement Labelled(Name label, AJCStatement body) {
        AJCLabeledStatement ret = new AJCLabeledStatement(javacTreeMaker.Labelled(label, body.getDecoratedTree()),
               body);

        body.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCSwitch Switch(AJCExpressionTree selector, List<AJCCase> cases) {
        AJCSwitch ret = new AJCSwitch(javacTreeMaker.Switch(selector.getDecoratedTree(), AJCTree.<JCCase, AJCCase>unwrap(cases)),
                selector, cases);

        selector.mParentNode = ret;
        for (AJCCase caze : cases) {
            caze.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCCase Case(AJCExpressionTree pat, List<AJCStatement> stats) {
        AJCCase ret = new AJCCase(javacTreeMaker.Case(pat.getDecoratedTree(), AJCCase.<JCStatement, AJCStatement>unwrap(stats)),
                pat, stats);

        pat.mParentNode = ret;
        for (AJCStatement stat : stats) {
            stat.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCSynchronized Synchronized(AJCExpressionTree lock, AJCBlock body) {
        AJCSynchronized ret = new AJCSynchronized(javacTreeMaker.Synchronized(lock.getDecoratedTree(), body.getDecoratedTree()),
                lock, body);

        lock.mParentNode = ret;
        body.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCTry Try(AJCBlock body, List<AJCCatch> catchers, AJCBlock finalizer) {
        AJCTry ret = new AJCTry(javacTreeMaker.Try(List.<JCTree>nil(), body.getDecoratedTree(), AJCTree.<JCCatch, AJCCatch>unwrap(catchers),
                finalizer.getDecoratedTree()),
                body, catchers, finalizer);

        body.mParentNode = ret;
        for (AJCCatch catcher : catchers) {
            catcher.mParentNode = ret;
        }
        finalizer.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCCatch Catch(AJCVariableDecl param, AJCBlock body) {
        AJCCatch ret = new AJCCatch(javacTreeMaker.Catch(param.getDecoratedTree(), body.getDecoratedTree()),
                param, body);

        param.mParentNode = ret;
        body.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCConditional Conditional(AJCExpressionTree cond, AJCExpressionTree thenpart, AJCExpressionTree elsepart) {
        AJCConditional ret = new AJCConditional(javacTreeMaker.Conditional(cond.getDecoratedTree(), thenpart.getDecoratedTree(), elsepart.getDecoratedTree()), cond, thenpart, elsepart);

        cond.mParentNode = ret;
        thenpart.mParentNode = ret;
        elsepart.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCIf If(AJCExpressionTree cond, AJCBlock thenpart, AJCBlock elsepart) {
        AJCIf ret = new AJCIf(javacTreeMaker.If(cond.getDecoratedTree(), thenpart.getDecoratedTree(), elsepart.getDecoratedTree()),
                cond, thenpart, elsepart);

        cond.mParentNode = ret;
        thenpart.mParentNode = ret;
        elsepart.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCExpressionStatement Exec(AJCExpressionTree expr) {
        AJCExpressionStatement ret = new AJCExpressionStatement(javacTreeMaker.Exec(expr.getDecoratedTree()), expr);

        expr.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCBreak Break(Name label) {
        return new AJCBreak(javacTreeMaker.Break(label));
    }

    @Override
    public AJCContinue Continue(Name label) {
        return new AJCContinue(javacTreeMaker.Continue(label));
    }

    @Override
    public AJCReturn Return(AJCExpressionTree expr) {
        AJCReturn ret = new AJCReturn(javacTreeMaker.Return(expr.getDecoratedTree()), expr);

        expr.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCThrow Throw(AJCExpressionTree expr) {
        AJCThrow ret = new AJCThrow(javacTreeMaker.Throw(expr.getDecoratedTree()), expr);

        expr.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCCall Call(AJCSymbolRefTree<MethodSymbol> fn, List<AJCExpressionTree> args) {
        AJCCall ret = new AJCCall(javacTreeMaker.Apply(List.<JCExpression>nil(), fn.getDecoratedTree(), AJCTree.<JCExpression, AJCExpressionTree>unwrap(args)),
                           fn, args);

        fn.mParentNode = ret;
        for (AJCExpressionTree arg : args) {
            arg.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCNewClass NewClass(AJCSymbolRefTree<ClassSymbol> clazz, List<AJCExpressionTree> args, AJCClassDecl def) {
        AJCNewClass ret = new AJCNewClass(javacTreeMaker.NewClass(null, List.<JCExpression>nil(),
                clazz.getDecoratedTree(), AJCTree.<JCExpression, AJCExpressionTree>unwrap(args), def.getDecoratedTree()),
                clazz, args, def);

        clazz.mParentNode = ret;
        for (AJCExpressionTree arg : args) {
            arg.mParentNode = ret;
        }
        def.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCNewArray NewArray(AJCTypeExpression elemtype, List<AJCExpressionTree> dims, List<AJCExpressionTree> elems) {
        AJCNewArray ret = new AJCNewArray(javacTreeMaker.NewArray(elemtype.getDecoratedTree(), AJCTree.<JCExpression, AJCExpressionTree>unwrap(dims), AJCTree.<JCExpression, AJCExpressionTree>unwrap(elems)),
                elemtype, dims, elems);

        elemtype.mParentNode = ret;
        for (AJCExpressionTree arg : dims) {
            arg.mParentNode = ret;
        }
        for (AJCExpressionTree arg : elems) {
            arg.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCAssign Assign(AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs) {
        AJCAssign ret = new AJCAssign(javacTreeMaker.Assign(lhs.getDecoratedTree(), rhs.getDecoratedTree()), lhs, rhs);

        lhs.mParentNode = ret;
        rhs.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCAssignOp Assignop(Tag opcode, AJCSymbolRefTree<VarSymbol> lhs, AJCExpressionTree rhs) {
        AJCAssignOp ret = new AJCAssignOp(javacTreeMaker.Assignop(opcode, lhs.getDecoratedTree(), rhs.getDecoratedTree()), lhs, rhs);

        lhs.mParentNode = ret;
        rhs.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCUnary Unary(Tag opcode, AJCExpressionTree arg) {
        AJCUnary ret = new AJCUnary(javacTreeMaker.Unary(opcode, arg.getDecoratedTree()), arg);

        arg.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCUnaryAsg UnaryAsg(Tag opcode, AJCSymbolRefTree<VarSymbol> arg) {
        if (opcode == Tag.PREINC
         || opcode == Tag.PREDEC
         || opcode == Tag.POSTINC
         || opcode == Tag.POSTDEC) {
            AJCUnaryAsg ret =  new AJCUnaryAsg(javacTreeMaker.Unary(opcode, arg.getDecoratedTree()), arg);

            arg.mParentNode = ret;

            return ret;
        }
        LogUtils.raiseCompilerError("Attempt to make UnaryAsg with invalid opcode!");
        return null;
    }

    @Override
    public AJCBinary Binary(Tag opcode, AJCExpressionTree lhs, AJCExpressionTree rhs) {
        AJCBinary ret = new AJCBinary(javacTreeMaker.Binary(opcode, lhs.getDecoratedTree(), rhs.getDecoratedTree()),
                lhs, rhs);

        lhs.mParentNode = ret;
        rhs.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCTypeCast TypeCast(AJCTypeExpression clazz, AJCExpressionTree expr) {
        AJCTypeCast ret = new AJCTypeCast(javacTreeMaker.TypeCast(clazz.getDecoratedTree(), expr.getDecoratedTree()), clazz, expr);

        clazz.mParentNode = ret;
        expr.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCInstanceOf InstanceOf(AJCSymbolRefTree<VarSymbol> expr, AJCSymbolRefTree<ClassSymbol> clazz) {
        AJCInstanceOf ret = new AJCInstanceOf(javacTreeMaker.TypeTest(expr.getDecoratedTree(), clazz.getDecoratedTree()), expr, clazz);

        expr.mParentNode = ret;
        clazz.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCArrayAccess ArrayAccess(AJCExpressionTree indexed, AJCExpressionTree index) {
        AJCArrayAccess ret = new AJCArrayAccess(javacTreeMaker.Indexed(indexed.getDecoratedTree(), index.getDecoratedTree()));

        indexed.mParentNode = ret;
        index.mParentNode = ret;

        return ret;
    }

    @Override
    public <T extends Symbol> AJCFieldAccess Select(AJCSymbolRefTree<T> selected, Name selector) {
        AJCFieldAccess ret = new AJCFieldAccess<>(javacTreeMaker.Select(selected.getDecoratedTree(), selector), selected);

        selected.mParentNode = ret;

        return ret;
    }

    @Override
    public <T extends Symbol> AJCFieldAccess Select(AJCSymbolRefTree<T> base, Symbol sym) {
        AJCFieldAccess ret = new AJCFieldAccess<>((JCFieldAccess) javacTreeMaker.Select(base.getDecoratedTree(), sym), base);

        base.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCIdent Ident(Name idname) {
        return new AJCIdent(javacTreeMaker.Ident(idname));
    }

    @Override
    public AJCIdent Ident(Symbol sym) {
        return new AJCIdent(javacTreeMaker.Ident(sym));
    }

    @Override
    public AJCLiteral Literal(TypeTag tag, Object value) {
        return new AJCLiteral(javacTreeMaker.Literal(tag, value));
    }

    @Override
    public AJCLiteral Literal(Object value) {
        return new AJCLiteral(javacTreeMaker.Literal(value));
    }

    @Override
    public AJCPrimitiveTypeTree TypeIdent(TypeTag typetag) {
        return new AJCPrimitiveTypeTree(javacTreeMaker.TypeIdent(typetag));
    }

    @Override
    public AJCArrayTypeTree TypeArray(AJCTypeExpression elemtype) {
        AJCArrayTypeTree ret = new AJCArrayTypeTree(javacTreeMaker.TypeArray(elemtype.getDecoratedTree()), elemtype);

        elemtype.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCTypeUnion TypeUnion(List<AJCTypeExpression> components) {
        AJCTypeUnion ret = new AJCTypeUnion(javacTreeMaker.TypeUnion(AJCTree.<JCExpression, AJCTypeExpression>unwrap(components)), components);

        for (AJCTypeExpression expr : components) {
            expr.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCAnnotation Annotation(AJCTree annotationType, List<AJCExpressionTree> args) {
        AJCAnnotation ret = new AJCAnnotation(javacTreeMaker.Annotation(annotationType.getDecoratedTree(), AJCTree.<JCExpression, AJCExpressionTree>unwrap(args)),
                annotationType, args);

        annotationType.mParentNode = ret;
        for (AJCExpressionTree expr : args) {
            expr.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCModifiers Modifiers(long flags, List<AJCAnnotation> annotations) {
        AJCModifiers ret = new AJCModifiers(javacTreeMaker.Modifiers(flags, AJCTree.<JCAnnotation, AJCAnnotation>unwrap(annotations)), annotations);

        for (AJCAnnotation anno : annotations) {
            anno.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCModifiers Modifiers(long flags) {
        return Modifiers(flags, List.<AJCAnnotation>nil());
    }

    @Override
    public AJCAnnotatedType AnnotatedType(AJCExpressionTree underlyingType) {
        AJCAnnotatedType ret = new AJCAnnotatedType(javacTreeMaker.AnnotatedType(List.<JCAnnotation>nil(), underlyingType.getDecoratedTree()), underlyingType);

        underlyingType.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCLetExpr LetExpr(List<AJCVariableDecl> defs, AJCExpressionTree expr) {
        AJCLetExpr ret = new AJCLetExpr(javacTreeMaker.LetExpr(AJCTree.<JCVariableDecl, AJCVariableDecl>unwrap(defs), expr.getDecoratedTree()),
                defs, expr);

        for (AJCVariableDecl def : defs) {
            def.mParentNode = ret;
        }
        expr.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCErroneous Erroneous(List<? extends AJCTree> errs) {
        return new AJCErroneous(javacTreeMaker.Erroneous(unwrap(errs)));
    }
}
