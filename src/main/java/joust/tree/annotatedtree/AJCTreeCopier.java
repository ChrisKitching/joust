package joust.tree.annotatedtree;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.StaticCompilerUtils.*;

public class AJCTreeCopier {
    protected static final Context.Key<AJCTreeCopier> AJCTreeCopierKey = new Context.Key<>();

    public static AJCTreeCopier instance(Context context) {
        AJCTreeCopier instance = context.get(AJCTreeCopierKey);
        if (instance == null) {
            instance = new AJCTreeCopier();
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T extends AJCTree> T copy(T that) {
        if (that == null) {
            return null;
        }

        if (that instanceof AJCImport) {
            return (T) copyImport((AJCImport) that);
        } else if (that instanceof AJCClassDecl) {
            return (T) copyClassDef((AJCClassDecl) that);
        } else if (that instanceof AJCMethodDecl) {
            return (T) copyMethodDef((AJCMethodDecl) that);
        } else if (that instanceof AJCVariableDecl) {
            return (T) copyVariableDecl((AJCVariableDecl) that);
        } else if (that instanceof AJCSkip) {
            return (T) copySkip((AJCSkip) that);
        } else if (that instanceof AJCBlock) {
            return (T) copyBlock((AJCBlock) that);
        } else if (that instanceof AJCDoWhileLoop) {
            return (T) copyDoWhileLoop((AJCDoWhileLoop) that);
        } else if (that instanceof AJCWhileLoop) {
            return (T) copyWhileLoop((AJCWhileLoop) that);
        } else if (that instanceof AJCForLoop) {
            return (T) copyForLoop((AJCForLoop) that);
        } else if (that instanceof AJCForEachLoop) {
            return (T) copyForeachLoop((AJCForEachLoop) that);
        } else if (that instanceof AJCLabeledStatement) {
            return (T) copyLabelledStatement((AJCLabeledStatement) that);
        } else if (that instanceof AJCSwitch) {
            return (T) copySwitch((AJCSwitch) that);
        } else if (that instanceof AJCCase) {
            return (T) copyCase((AJCCase) that);
        } else if (that instanceof AJCSynchronized) {
            return (T) copySynchronized((AJCSynchronized) that);
        } else if (that instanceof AJCTry) {
            return (T) copyTry((AJCTry) that);
        } else if (that instanceof AJCCatch) {
            return (T) copyCatch((AJCCatch) that);
        } else if (that instanceof AJCConditional) {
            return (T) copyConditional((AJCConditional) that);
        } else if (that instanceof AJCIf) {
            return (T) copyIf((AJCIf) that);
        } else if (that instanceof AJCExpressionStatement) {
            return (T) copyExpressionStatement((AJCExpressionStatement) that);
        } else if (that instanceof AJCBreak) {
            return (T) copyBreak((AJCBreak) that);
        } else if (that instanceof AJCContinue) {
            return (T) copyContinue((AJCContinue) that);
        } else if (that instanceof AJCReturn) {
            return (T) copyReturn((AJCReturn) that);
        } else if (that instanceof AJCThrow) {
            return (T) copyThrow((AJCThrow) that);
        } else if (that instanceof AJCAssert) {
            return (T) copyAssert((AJCAssert) that);
        } else if (that instanceof AJCCall) {
            return (T) copyCall((AJCCall) that);
        } else if (that instanceof AJCNewClass) {
            return (T) copyNewClass((AJCNewClass) that);
        } else if (that instanceof AJCNewArray) {
            return (T) copyNewArray((AJCNewArray) that);
        } else if (that instanceof AJCLambda) {
            return (T) copyLambda((AJCLambda) that);
        } else if (that instanceof AJCAssign) {
            return (T) copyAssign((AJCAssign) that);
        } else if (that instanceof AJCAssignOp) {
            return (T) copyAssignop((AJCAssignOp) that);
        } else if (that instanceof AJCUnary) {
            return (T) copyUnary((AJCUnary) that);
        } else if (that instanceof AJCUnaryAsg) {
            return (T) copyUnaryAsg((AJCUnaryAsg) that);
        } else if (that instanceof AJCBinary) {
            return (T) copyBinary((AJCBinary) that);
        } else if (that instanceof AJCTypeCast) {
            return (T) copyTypeCast((AJCTypeCast) that);
        } else if (that instanceof AJCInstanceOf) {
            return (T) copyInstanceOf((AJCInstanceOf) that);
        } else if (that instanceof AJCArrayAccess) {
            return (T) copyArrayAccess((AJCArrayAccess) that);
        } else if (that instanceof AJCFieldAccess) {
            return (T) copyFieldAccess((AJCFieldAccess) that);
        } else if (that instanceof AJCMemberReference) {
            return (T) copyMemberReference((AJCMemberReference) that);
        } else if (that instanceof AJCIdent) {
            return (T) copyIdent((AJCIdent) that);
        } else if (that instanceof AJCLiteral) {
            return (T) copyLiteral((AJCLiteral) that);
        } else if (that instanceof AJCPrimitiveTypeTree) {
            return (T) copyPrimitiveType((AJCPrimitiveTypeTree) that);
        } else if (that instanceof AJCArrayTypeTree) {
            return (T) copyArrayType((AJCArrayTypeTree) that);
        } else if (that instanceof AJCTypeApply) {
            return (T) copyTypeApply((AJCTypeApply) that);
        } else if (that instanceof AJCTypeUnion) {
            return (T) copyTypeUnion((AJCTypeUnion) that);
        } else if (that instanceof AJCTypeIntersection) {
            return (T) copyTypeIntersection((AJCTypeIntersection) that);
        } else if (that instanceof AJCTypeParameter) {
            return (T) copyTypeParameter((AJCTypeParameter) that);
        } else if (that instanceof AJCWildcard) {
            return (T) copyWildcard((AJCWildcard) that);
        } else if (that instanceof AJCTypeBoundKind) {
            return (T) copyTypeBoundKind((AJCTypeBoundKind) that);
        } else if (that instanceof AJCAnnotation) {
            return (T) copyAnnotation((AJCAnnotation) that);
        } else if (that instanceof AJCModifiers) {
            return (T) copyModifiers((AJCModifiers) that);
        } else if (that instanceof AJCAnnotatedType) {
            return (T) copyAnnotatedType((AJCAnnotatedType) that);
        } else if (that instanceof AJCLetExpr) {
            return (T) copyLetExpr((AJCLetExpr) that);
        } else if (that instanceof AJCEmptyExpression) {
            return (T) copyEmptyExpression((AJCEmptyExpression) that);
        } else if (that instanceof AJCErroneous) {
            return (T) copyErroneous((AJCErroneous) that);
        }

        return null;
    }

    /**
     * Copy a list of nodes...
     * @param trees
     * @return
     */
    public <T extends AJCTree> List<T> copy(List<T> trees) {
        if (trees == null) {
            return null;
        }

        List<T> ret = List.nil();

        for (T t : trees) {
            ret = ret.prepend(this.<T>copy(t));
        }

        return ret.reverse();
    }

    public AJCImport copyImport(AJCImport that) {
        AJCImport imp = treeMaker.Import(copy(that.qualid), that.isStatic());
        imp.getDecoratedTree().type = that.getDecoratedTree().type;
        return imp;
    }

    
    public AJCClassDecl copyClassDef(AJCClassDecl that) {
        AJCClassDecl classDecl = treeMaker.ClassDef(copy(that.mods),
                                                    that.getDecoratedTree().name,
                                                    copy(that.typarams),
                                                    copy(that.extending),
                                                    copy(that.implementing),
                                                    copy(that.fields),
                                                    copy(that.methods),
                                                    copy(that.classes));

        classDecl.getDecoratedTree().type = that.getDecoratedTree().type;
        classDecl.getDecoratedTree().sym = that.getDecoratedTree().sym;

        return classDecl;
    }

    
    public AJCMethodDecl copyMethodDef(AJCMethodDecl that) {
        AJCMethodDecl methodDecl = treeMaker.MethodDef(copy(that.mods),
                                                       that.getDecoratedTree().name,
                                                       copy(that.restype),
                                                       copy(that.typarams),
                                                       copy(that.recvparam),
                                                       copy(that.params),
                                                       copy(that.thrown),
                                                       copy(that.body),
                                                       copy(that.defaultValue));

        methodDecl.getDecoratedTree().type = that.getDecoratedTree().type;
        methodDecl.getDecoratedTree().sym = that.getDecoratedTree().sym;

        return methodDecl;
    }

    
    public AJCVariableDecl copyVariableDecl(AJCVariableDecl that) {
        AJCVariableDecl varDecl = treeMaker.VarDef(copy(that.mods), that.getName(), copy(that.vartype), copy(that.init));

        varDecl.getDecoratedTree().type = that.getDecoratedTree().type;
        varDecl.getDecoratedTree().sym = that.getDecoratedTree().sym;

        return varDecl;
    }

    
    public AJCSkip copySkip(AJCSkip that) {
        AJCSkip skip = treeMaker.Skip();
        skip.getDecoratedTree().type = that.getDecoratedTree().type;
        return skip;
    }

    
    public AJCEmptyExpression copyEmptyExpression(AJCEmptyExpression that) {
        return treeMaker.EmptyExpression();
    }
    
    public AJCBlock copyBlock(AJCBlock that) {
        AJCBlock block = treeMaker.Block(that.getDecoratedTree().flags, copy(that.stats));
        block.getDecoratedTree().type = that.getDecoratedTree().type;
        return block;
    }

    
    public AJCDoWhileLoop copyDoWhileLoop(AJCDoWhileLoop that) {
        AJCDoWhileLoop loop = treeMaker.DoLoop(copy(that.body), copy(that.cond));

        loop.getDecoratedTree().type = that.getDecoratedTree().type;
        return loop;
    }

    
    public AJCWhileLoop copyWhileLoop(AJCWhileLoop that) {
        AJCWhileLoop loop = treeMaker.WhileLoop(copy(that.cond), copy(that.body));

        loop.getDecoratedTree().type = that.getDecoratedTree().type;
        return loop;
    }

    
    public AJCForLoop copyForLoop(AJCForLoop that) {
        AJCForLoop loop = treeMaker.ForLoop(copy(that.init), copy(that.cond), copy(that.step), copy(that.body));

        loop.getDecoratedTree().type = that.getDecoratedTree().type;
        return loop;
    }

    
    public AJCForEachLoop copyForeachLoop(AJCForEachLoop that) {
        AJCForEachLoop loop = treeMaker.ForeachLoop(copy(that.var), copy(that.expr), copy(that.body));

        loop.getDecoratedTree().type = that.getDecoratedTree().type;
        return loop;
    }

    
    public AJCLabeledStatement copyLabelledStatement(AJCLabeledStatement that) {
        AJCLabeledStatement node = treeMaker.Labelled(that.getDecoratedTree().label, copy(that.body));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCSwitch copySwitch(AJCSwitch that) {
        AJCSwitch node = treeMaker.Switch(copy(that.selector), copy(that.cases));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCCase copyCase(AJCCase that) {
        AJCCase node = treeMaker.Case(copy(that.pat), copy(that.stats));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCSynchronized copySynchronized(AJCSynchronized that) {
        AJCSynchronized node = treeMaker.Synchronized(copy(that.lock), copy(that.body));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCTry copyTry(AJCTry that) {
        AJCTry node = treeMaker.Try(copy(that.resources), copy(that.body), copy(that.catchers), copy(that.finalizer));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCCatch copyCatch(AJCCatch that) {
        AJCCatch node = treeMaker.Catch(copy(that.param), copy(that.body));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCConditional copyConditional(AJCConditional that) {
        AJCConditional node = treeMaker.Conditional(copy(that.cond), copy(that.truepart), copy(that.falsepart));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCIf copyIf(AJCIf that) {
        AJCIf node = treeMaker.If(copy(that.cond), copy(that.thenpart), copy(that.elsepart));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCExpressionStatement copyExpressionStatement(AJCExpressionStatement that) {
        AJCExpressionStatement node = treeMaker.Exec(copy(that.expr));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCBreak copyBreak(AJCBreak that) {
        AJCBreak node = treeMaker.Break(that.getDecoratedTree().label);

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        node.getDecoratedTree().target = that.getDecoratedTree().target;
        return node;
    }

    
    public AJCContinue copyContinue(AJCContinue that) {
        AJCContinue node = treeMaker.Continue(that.getDecoratedTree().label);

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        node.getDecoratedTree().target = that.getDecoratedTree().target;
        return node;
    }

    
    public AJCReturn copyReturn(AJCReturn that) {
        AJCReturn node = treeMaker.Return(copy(that.expr));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCThrow copyThrow(AJCThrow that) {
        AJCThrow node = treeMaker.Throw(copy(that.expr));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCAssert copyAssert(AJCAssert that) {
        AJCAssert node = treeMaker.Assert(copy(that.cond), copy(that.detail));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCCall copyCall(AJCCall that) {
        AJCCall node = treeMaker.Call(copy(that.typeargs), copy(that.meth), copy(that.args));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCNewClass copyNewClass(AJCNewClass that) {
        AJCNewClass node = treeMaker.NewClass(copy(that.encl),
                copy(that.typeargs),
                copy(that.clazz),
                copy(that.args),
                copy(that.def));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCNewArray copyNewArray(AJCNewArray that) {
        AJCNewArray node = treeMaker.NewArray(copy(that.elemtype), copy(that.dims), copy(that.elems));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCLambda copyLambda(AJCLambda that) {
        throw new UnsupportedOperationException("Lambdas not supported.");
    }

    
    public AJCAssign copyAssign(AJCAssign that) {
        AJCAssign node = treeMaker.Assign(copy(that.lhs), copy(that.rhs));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCAssignOp copyAssignop(AJCAssignOp that) {
        AJCAssignOp node = treeMaker.Assignop(that.getTag(), copy(that.lhs), copy(that.rhs));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        node.getDecoratedTree().operator = that.getDecoratedTree().operator;
        return node;
    }

    
    public AJCUnary copyUnary(AJCUnary that) {
        AJCUnary node = treeMaker.Unary(that.getTag(), copy(that.arg));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        node.getDecoratedTree().operator = that.getDecoratedTree().operator;
        return node;
    }

    
    public AJCUnaryAsg copyUnaryAsg(AJCUnaryAsg that) {
        AJCUnaryAsg node = treeMaker.UnaryAsg(that.getTag(), copy(that.arg));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        node.getDecoratedTree().operator = that.getDecoratedTree().operator;
        return node;
    }

    
    public AJCBinary copyBinary(AJCBinary that) {
        AJCBinary node = treeMaker.Binary(that.getTag(), copy(that.lhs), copy(that.rhs));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        node.getDecoratedTree().operator = that.getDecoratedTree().operator;
        return node;
    }

    
    public AJCTypeCast copyTypeCast(AJCTypeCast that) {
        AJCTypeCast node = treeMaker.TypeCast(copy(that.clazz), copy(that.expr));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCInstanceOf copyInstanceOf(AJCInstanceOf that) {
        AJCInstanceOf node = treeMaker.InstanceOf(copy(that.expr), copy(that.clazz));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCArrayAccess copyArrayAccess(AJCArrayAccess that) {
        AJCArrayAccess node = treeMaker.ArrayAccess(copy(that.indexed), copy(that.index));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCFieldAccess copyFieldAccess(AJCFieldAccess that) {
        AJCFieldAccess node = treeMaker.Select(copy(that.selected), that.getDecoratedTree().getIdentifier());

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        node.getDecoratedTree().sym = that.getDecoratedTree().sym;
        return node;
    }

    
    public AJCMemberReference copyMemberReference(AJCMemberReference that) {
        throw new UnsupportedOperationException("Member references not supported.");
    }

    
    public AJCIdent copyIdent(AJCIdent that) {
        AJCIdent node = treeMaker.Ident(that.getTargetSymbol());

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        node.getDecoratedTree().sym = that.getDecoratedTree().sym;
        return node;
    }
    
    public AJCLiteral copyLiteral(AJCLiteral that) {
        AJCLiteral node = treeMaker.Literal(that.getDecoratedTree().typetag, that.getValue());

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    public AJCPrimitiveTypeTree copyPrimitiveType(AJCPrimitiveTypeTree that) {
        AJCPrimitiveTypeTree node = treeMaker.TypeIdent(that.getDecoratedTree().typetag);

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCArrayTypeTree copyArrayType(AJCArrayTypeTree that) {
        AJCArrayTypeTree node = treeMaker.TypeArray(copy(that.elemtype));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCTypeApply copyTypeApply(AJCTypeApply that) {
        AJCTypeApply node = treeMaker.TypeApply(copy(that.clazz), copy(that.arguments));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCTypeUnion copyTypeUnion(AJCTypeUnion that) {
        AJCTypeUnion node = treeMaker.TypeUnion(copy(that.alternatives));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCTypeIntersection copyTypeIntersection(AJCTypeIntersection that) {
        AJCTypeIntersection node = treeMaker.TypeIntersection(copy(that.bounds));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCTypeParameter copyTypeParameter(AJCTypeParameter that) {
        AJCTypeParameter node = treeMaker.TypeParameter(that.getName(), copy(that.bounds), copy(that.annotations));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCWildcard copyWildcard(AJCWildcard that) {
        AJCWildcard node = treeMaker.Wildcard(copy(that.kind), copy(that.inner));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCTypeBoundKind copyTypeBoundKind(AJCTypeBoundKind that) {
        AJCTypeBoundKind node = treeMaker.TypeBoundKind(that.getDecoratedTree().kind);

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCAnnotation copyAnnotation(AJCAnnotation that) {
        AJCAnnotation node = treeMaker.Annotation(copy(that.annotationType), copy(that.args));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCModifiers copyModifiers(AJCModifiers that) {
        AJCModifiers node = treeMaker.Modifiers(that.getDecoratedTree().flags, copy(that.annotations));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    
    public AJCAnnotatedType copyAnnotatedType(AJCAnnotatedType that) {
        AJCAnnotatedType node = treeMaker.AnnotatedType(copy(that.annotations), copy(that.underlyingType));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }

    public AJCLetExpr copyLetExpr(AJCLetExpr that) {
        AJCLetExpr node = treeMaker.LetExpr(copy(that.defs), copy(that.expr));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }
    
    public AJCErroneous copyErroneous(AJCErroneous that) {
        AJCErroneous node = new AJCErroneous(javacTreeMaker.Erroneous(that.getDecoratedTree().errs));

        node.getDecoratedTree().type = that.getDecoratedTree().type;
        return node;
    }
}
