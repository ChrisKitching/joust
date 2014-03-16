package joust.tree.annotatedtree;

import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import joust.utils.LogUtils;


import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTag.*;
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
    public AJCImport Import(AJCTree qualid, boolean staticImport) {
        AJCImport ret = new AJCImport(javacTreeMaker.Import(qualid.getDecoratedTree(), staticImport), qualid);

        qualid.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCClassDecl ClassDef(AJCModifiers mods, Name name, List<AJCTypeParameter> typarams, AJCExpression extending,
                                 List<AJCExpression> implementing,  List<AJCVariableDecl> fields, List<AJCMethodDecl> methods, List<AJCClassDecl> classes) {
        AJCClassDecl ret = new AJCClassDecl(javacTreeMaker.ClassDef(mods.<JCModifiers, AJCModifiers>getDecoratedTree(), name, AJCTree.<JCTypeParameter, AJCTypeParameter>unwrap(typarams), extending.getDecoratedTree(),
                AJCTree.<JCExpression, AJCExpression>unwrap(implementing), unwrap(fields).prependList(unwrap(methods)).prependList(unwrap(classes))),
                mods, typarams, extending, implementing, fields, methods, classes);

        mods.mParentNode = ret;
        for (AJCTypeParameter param : typarams) {
            param.mParentNode = ret;
        }
        extending.mParentNode = ret;
        for (AJCExpression expr : implementing) {
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
    public AJCMethodDecl MethodDef(AJCModifiers mods, Name name, AJCExpression restype, List<AJCTypeParameter> typarams, AJCVariableDecl recvparam, List<AJCVariableDecl> params, List<AJCExpression> thrown, AJCBlock body, AJCExpression defaultValue) {
        AJCMethodDecl ret = new AJCMethodDecl(javacTreeMaker.MethodDef(mods.<JCModifiers, AJCModifiers>getDecoratedTree(), name, restype.getDecoratedTree(), AJCTree.<JCTypeParameter, AJCTypeParameter>unwrap(typarams), recvparam.getDecoratedTree(), AJCTree.<JCVariableDecl, AJCVariableDecl>unwrap(params), AJCTree.<JCExpression, AJCExpression>unwrap(thrown), body.getDecoratedTree(), defaultValue.getDecoratedTree()),
                mods, restype, typarams, recvparam, params, thrown, body, defaultValue);

        mods.mParentNode = ret;
        restype.mParentNode = ret;
        for (AJCTypeParameter param : typarams) {
            param.mParentNode = ret;
        }
        recvparam.mParentNode = ret;
        for (AJCVariableDecl param : params) {
            param.mParentNode = ret;
        }
        for (AJCExpression thro : thrown) {
            thro.mParentNode = ret;
        }
        body.mParentNode = ret;
        defaultValue.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCMethodDecl MethodDef(AJCModifiers mods, Name name, AJCExpression restype, List<AJCTypeParameter> typarams, List<AJCVariableDecl> params, List<AJCExpression> thrown, AJCBlock body, AJCExpression defaultValue) {
        return MethodDef(mods, name, restype, typarams, null, params, thrown, body, defaultValue);
    }

    @Override
    public AJCVariableDecl VarDef(AJCModifiers mods, Name name, AJCTypeExpression vartype, AJCExpression init) {
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
    public AJCDoWhileLoop DoLoop(AJCBlock body, AJCExpression cond) {
        AJCDoWhileLoop ret = new AJCDoWhileLoop(javacTreeMaker.DoLoop(body.getDecoratedTree(), cond.getDecoratedTree()),
                body, cond);
        body.mParentNode = ret;
        cond.mParentNode = ret;
        return ret;
    }

    @Override
    public AJCWhileLoop WhileLoop(AJCExpression cond, AJCBlock body) {
        AJCWhileLoop ret = new AJCWhileLoop(javacTreeMaker.WhileLoop(cond.getDecoratedTree(), body.getDecoratedTree()),
                cond, body);

        cond.mParentNode = ret;
        body.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCForLoop ForLoop(List<AJCStatement> init, AJCExpression cond, List<AJCExpressionStatement> step, AJCBlock body) {
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
    public AJCForEachLoop ForeachLoop(AJCVariableDecl var, AJCExpression expr, AJCBlock body) {
        AJCForEachLoop ret = new AJCForEachLoop(javacTreeMaker.ForeachLoop(var.getDecoratedTree(), expr.getDecoratedTree(), body.getDecoratedTree()),
                var, expr, body);

        var.mParentNode = ret;
        expr.mParentNode = ret;
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
    public AJCSwitch Switch(AJCExpression selector, List<AJCCase> cases) {
        AJCSwitch ret = new AJCSwitch(javacTreeMaker.Switch(selector.getDecoratedTree(), AJCTree.<JCCase, AJCCase>unwrap(cases)),
                selector, cases);

        selector.mParentNode = ret;
        for (AJCCase caze : cases) {
            caze.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCCase Case(AJCExpression pat, List<AJCStatement> stats) {
        AJCCase ret = new AJCCase(javacTreeMaker.Case(pat.getDecoratedTree(), AJCCase.<JCStatement, AJCStatement>unwrap(stats)),
                pat, stats);

        pat.mParentNode = ret;
        for (AJCStatement stat : stats) {
            stat.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCSynchronized Synchronized(AJCExpression lock, AJCBlock body) {
        AJCSynchronized ret = new AJCSynchronized(javacTreeMaker.Synchronized(lock.getDecoratedTree(), body.getDecoratedTree()),
                lock, body);

        lock.mParentNode = ret;
        body.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCTry Try(AJCBlock body, List<AJCCatch> catchers, AJCBlock finalizer) {
        return Try(List.<AJCEffectAnnotatedTree>nil(), body, catchers, finalizer);
    }

    @Override
    public AJCTry Try(List<AJCEffectAnnotatedTree> resources, AJCBlock body, List<AJCCatch> catchers, AJCBlock finalizer) {
        AJCTry ret = new AJCTry(javacTreeMaker.Try(unwrap(resources), body.getDecoratedTree(), AJCTree.<JCCatch, AJCCatch>unwrap(catchers),
                finalizer.getDecoratedTree()),
                resources, body, catchers, finalizer);

        for (AJCEffectAnnotatedTree res : resources) {
            res.mParentNode = ret;
        }
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
    public AJCConditional Conditional(AJCExpression cond, AJCExpression thenpart, AJCExpression elsepart) {
        AJCConditional ret = new AJCConditional(javacTreeMaker.Conditional(cond.getDecoratedTree(), thenpart.getDecoratedTree(), elsepart.getDecoratedTree()), cond, thenpart, elsepart);

        cond.mParentNode = ret;
        thenpart.mParentNode = ret;
        elsepart.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCIf If(AJCExpression cond, AJCBlock thenpart, AJCBlock elsepart) {
        AJCIf ret = new AJCIf(javacTreeMaker.If(cond.getDecoratedTree(), thenpart.getDecoratedTree(), elsepart.getDecoratedTree()),
                cond, thenpart, elsepart);

        cond.mParentNode = ret;
        thenpart.mParentNode = ret;
        elsepart.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCExpressionStatement Exec(AJCExpression expr) {
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
    public AJCReturn Return(AJCExpression expr) {
        AJCReturn ret = new AJCReturn(javacTreeMaker.Return(expr.getDecoratedTree()), expr);

        expr.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCThrow Throw(AJCExpression expr) {
        AJCThrow ret = new AJCThrow(javacTreeMaker.Throw(expr.getDecoratedTree()), expr);

        expr.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCAssert Assert(AJCExpression cond, AJCExpression detail) {
        AJCAssert ret = new AJCAssert(javacTreeMaker.Assert(cond.getDecoratedTree(), detail.getDecoratedTree()),
                cond, detail);

        cond.mParentNode = ret;
        detail.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCCall Call(List<AJCExpression> typeargs, AJCSymbolRefTree<MethodSymbol> fn, List<AJCExpression> args) {
        AJCCall ret = new AJCCall(javacTreeMaker.Apply(AJCTree.<JCExpression, AJCExpression>unwrap(typeargs), fn.getDecoratedTree(), AJCTree.<JCExpression, AJCExpression>unwrap(args)),
                           typeargs, fn, args);

        for (AJCExpression expr : typeargs) {
            expr.mParentNode = ret;
        }
        fn.mParentNode = ret;
        for (AJCExpression arg : args) {
            arg.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCNewClass NewClass(AJCExpression encl, List<AJCExpression> typeargs, AJCExpression clazz, List<AJCExpression> args, AJCClassDecl def) {
        AJCNewClass ret = new AJCNewClass(javacTreeMaker.NewClass(encl.getDecoratedTree(), AJCTree.<JCExpression, AJCExpression>unwrap(typeargs),
                clazz.getDecoratedTree(), AJCTree.<JCExpression, AJCExpression>unwrap(args), def.getDecoratedTree()),
               encl, typeargs, clazz, args, def);

        encl.mParentNode = encl;
        for (AJCExpression expr : typeargs) {
            expr.mParentNode = ret;
        }
        clazz.mParentNode = ret;
        for (AJCExpression arg : args) {
            arg.mParentNode = ret;
        }
        def.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCNewArray NewArray(AJCExpression elemtype, List<AJCExpression> dims, List<AJCExpression> elems) {
        AJCNewArray ret = new AJCNewArray(javacTreeMaker.NewArray(elemtype.getDecoratedTree(), AJCTree.<JCExpression, AJCExpression>unwrap(dims), AJCTree.<JCExpression, AJCExpression>unwrap(elems)),
                elemtype, dims, elems);

        elemtype.mParentNode = ret;
        for (AJCExpression arg : dims) {
            arg.mParentNode = ret;
        }
        for (AJCExpression arg : elems) {
            arg.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCAssign Assign(AJCSymbolRefTree<VarSymbol> lhs, AJCExpression rhs) {
        AJCAssign ret = new AJCAssign(javacTreeMaker.Assign(lhs.getDecoratedTree(), rhs.getDecoratedTree()), lhs, rhs);

        lhs.mParentNode = ret;
        rhs.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCAssignOp Assignop(Tag opcode, AJCSymbolRefTree<VarSymbol> lhs, AJCExpression rhs) {
        AJCAssignOp ret = new AJCAssignOp(javacTreeMaker.Assignop(opcode, lhs.getDecoratedTree(), rhs.getDecoratedTree()), lhs, rhs);

        lhs.mParentNode = ret;
        rhs.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCUnary Unary(Tag opcode, AJCExpression arg) {
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
    public AJCBinary Binary(Tag opcode, AJCExpression lhs, AJCExpression rhs) {
        AJCBinary ret = new AJCBinary(javacTreeMaker.Binary(opcode, lhs.getDecoratedTree(), rhs.getDecoratedTree()),
                lhs, rhs);

        lhs.mParentNode = ret;
        rhs.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCTypeCast TypeCast(AJCTree clazz, AJCExpression expr) {
        AJCTypeCast ret = new AJCTypeCast(javacTreeMaker.TypeCast(clazz.getDecoratedTree(), expr.getDecoratedTree()), clazz, expr);

        clazz.mParentNode = ret;
        expr.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCInstanceOf InstanceOf(AJCSymbolRefTree<VarSymbol> expr, AJCTree clazz) {
        AJCInstanceOf ret = new AJCInstanceOf(javacTreeMaker.TypeTest(expr.getDecoratedTree(), clazz.getDecoratedTree()), expr, clazz);

        expr.mParentNode = ret;
        clazz.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCArrayAccess ArrayAccess(AJCExpression indexed, AJCExpression index) {
        AJCArrayAccess ret = new AJCArrayAccess(javacTreeMaker.Indexed(indexed.getDecoratedTree(), index.getDecoratedTree()));

        indexed.mParentNode = ret;
        index.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCFieldAccess Select(AJCSymbolRefTree selected, Name selector) {
        AJCFieldAccess ret = new AJCFieldAccess(javacTreeMaker.Select(selected.getDecoratedTree(), selector), selected);

        selected.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCFieldAccess Select(AJCSymbolRefTree base, Symbol sym) {
        AJCFieldAccess ret = new AJCFieldAccess((JCFieldAccess) javacTreeMaker.Select(base.getDecoratedTree(), sym), base);

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
    public AJCArrayTypeTree TypeArray(AJCExpression elemtype) {
        AJCArrayTypeTree ret = new AJCArrayTypeTree(javacTreeMaker.TypeArray(elemtype.getDecoratedTree()), elemtype);

        elemtype.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCTypeApply TypeApply(AJCExpression clazz, List<AJCExpression> arguments) {
        AJCTypeApply ret = new AJCTypeApply(javacTreeMaker.TypeApply(clazz.getDecoratedTree(), AJCTree.<JCExpression, AJCExpression>unwrap(arguments)),
                clazz, arguments);

        clazz.mParentNode = ret;
        for (AJCExpression expr : arguments) {
            expr.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCTypeUnion TypeUnion(List<AJCExpression> components) {
        AJCTypeUnion ret = new AJCTypeUnion(javacTreeMaker.TypeUnion(AJCTree.<JCExpression, AJCExpression>unwrap(components)), components);

        for (AJCExpression expr : components) {
            expr.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCTypeIntersection TypeIntersection(List<AJCExpression> components) {
        AJCTypeIntersection ret = new AJCTypeIntersection(javacTreeMaker.TypeIntersection(AJCTree.<JCExpression, AJCExpression>unwrap(components)), components);

        for (AJCExpression expr : components) {
            expr.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCTypeParameter TypeParameter(Name name, List<AJCExpression> bounds) {
        AJCTypeParameter ret = TypeParameter(name, bounds, List.<AJCAnnotation>nil());

        for (AJCExpression expr : bounds) {
            expr.mParentNode = ret;
        }

        return ret;
    }

    // TODO: Can we eliminate this?
    @Override
    public AJCTypeParameter TypeParameter(Name name, List<AJCExpression> bounds, List<AJCAnnotation> annos) {
        AJCTypeParameter ret = new AJCTypeParameter(javacTreeMaker.TypeParameter(name, AJCTree.<JCExpression, AJCExpression>unwrap(bounds), AJCTree.<JCAnnotation, AJCAnnotation>unwrap(annos)),
                bounds, annos);

        for (AJCExpression expr : bounds) {
            expr.mParentNode = ret;
        }
        for (AJCAnnotation anno : annos) {
            anno.mParentNode = ret;
        }

        return ret;
    }

    @Override
    public AJCWildcard Wildcard(AJCTypeBoundKind kind, AJCTree type) {
        AJCWildcard ret = new AJCWildcard(javacTreeMaker.Wildcard(kind.getDecoratedTree(), type.getDecoratedTree()),
                kind, type);

        kind.mParentNode = ret;
        type.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCTypeBoundKind TypeBoundKind(BoundKind kind) {
        return new AJCTypeBoundKind(javacTreeMaker.TypeBoundKind(kind));
    }

    @Override
    public AJCAnnotation Annotation(AJCTree annotationType, List<AJCExpression> args) {
        AJCAnnotation ret = new AJCAnnotation(javacTreeMaker.Annotation(annotationType.getDecoratedTree(), AJCTree.<JCExpression, AJCExpression>unwrap(args)),
                annotationType, args);

        annotationType.mParentNode = ret;
        for (AJCExpression expr : args) {
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
    public AJCAnnotatedType AnnotatedType(List<AJCAnnotation> annotations, AJCExpression underlyingType) {
        AJCAnnotatedType ret = new AJCAnnotatedType(javacTreeMaker.AnnotatedType(AJCTree.<JCAnnotation, AJCAnnotation>unwrap(annotations), underlyingType.getDecoratedTree()), annotations, underlyingType);

        for (AJCAnnotation anno : annotations) {
            anno.mParentNode = ret;
        }
        underlyingType.mParentNode = ret;

        return ret;
    }

    @Override
    public AJCLetExpr LetExpr(List<AJCVariableDecl> defs, AJCExpression expr) {
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
