package joust.tree.annotatedtree;

import com.sun.tools.javac.util.List;

import static joust.tree.annotatedtree.AJCTree.*;

/**
 * The default implementation of the tree visitor interface. Traverses the tree in a depth-first manner.
 * Differs notably from javac's tree traversers in that it doesn't put the traversal logic in the tree nodes
 * themselves.
 */
public abstract class AJCTreeVisitorImpl implements AJCTreeVisitor {
    // TODO: Something less stupid than this.
    public void visit(AJCTree that) {
        if (that == null) {
            return;
        }

        if (that instanceof AJCImport) {
            visitImport((AJCImport) that);
        } else if (that instanceof AJCClassDecl) {
            visitClassDef((AJCClassDecl) that);
        } else if (that instanceof AJCMethodDecl) {
            visitMethodDef((AJCMethodDecl) that);
        } else if (that instanceof AJCVariableDecl) {
            visitVariableDecl((AJCVariableDecl) that);
        } else if (that instanceof AJCSkip) {
            visitSkip((AJCSkip) that);
        } else if (that instanceof AJCBlock) {
            visitBlock((AJCBlock) that);
        } else if (that instanceof AJCDoWhileLoop) {
            visitDoWhileLoop((AJCDoWhileLoop) that);
        } else if (that instanceof AJCWhileLoop) {
            visitWhileLoop((AJCWhileLoop) that);
        } else if (that instanceof AJCForLoop) {
            visitForLoop((AJCForLoop) that);
        } else if (that instanceof AJCForEachLoop) {
            visitForeachLoop((AJCForEachLoop) that);
        } else if (that instanceof AJCLabeledStatement) {
            visitLabelledStatement((AJCLabeledStatement) that);
        } else if (that instanceof AJCSwitch) {
            visitSwitch((AJCSwitch) that);
        } else if (that instanceof AJCCase) {
            visitCase((AJCCase) that);
        } else if (that instanceof AJCSynchronized) {
            visitSynchronized((AJCSynchronized) that);
        } else if (that instanceof AJCTry) {
            visitTry((AJCTry) that);
        } else if (that instanceof AJCCatch) {
            visitCatch((AJCCatch) that);
        } else if (that instanceof AJCConditional) {
            visitConditional((AJCConditional) that);
        } else if (that instanceof AJCIf) {
            visitIf((AJCIf) that);
        } else if (that instanceof AJCExpressionStatement) {
            visitExpressionStatement((AJCExpressionStatement) that);
        } else if (that instanceof AJCBreak) {
            visitBreak((AJCBreak) that);
        } else if (that instanceof AJCContinue) {
            visitContinue((AJCContinue) that);
        } else if (that instanceof AJCReturn) {
            visitReturn((AJCReturn) that);
        } else if (that instanceof AJCThrow) {
            visitThrow((AJCThrow) that);
        } else if (that instanceof AJCAssert) {
            visitAssert((AJCAssert) that);
        } else if (that instanceof AJCCall) {
            visitCall((AJCCall) that);
        } else if (that instanceof AJCNewClass) {
            visitNewClass((AJCNewClass) that);
        } else if (that instanceof AJCNewArray) {
            visitNewArray((AJCNewArray) that);
        } else if (that instanceof AJCLambda) {
            visitLambda((AJCLambda) that);
        } else if (that instanceof AJCAssign) {
            visitAssign((AJCAssign) that);
        } else if (that instanceof AJCAssignOp) {
            visitAssignop((AJCAssignOp) that);
        } else if (that instanceof AJCUnary) {
            visitUnary((AJCUnary) that);
        } else if (that instanceof AJCUnaryAsg) {
            visitUnaryAsg((AJCUnaryAsg) that);
        } else if (that instanceof AJCBinary) {
            visitBinary((AJCBinary) that);
        } else if (that instanceof AJCTypeCast) {
            visitTypeCast((AJCTypeCast) that);
        } else if (that instanceof AJCInstanceOf) {
            visitInstanceOf((AJCInstanceOf) that);
        } else if (that instanceof AJCArrayAccess) {
            visitArrayAccess((AJCArrayAccess) that);
        } else if (that instanceof AJCFieldAccess) {
            visitFieldAccess((AJCFieldAccess) that);
        } else if (that instanceof AJCMemberReference) {
            visitMemberReference((AJCMemberReference) that);
        } else if (that instanceof AJCIdent) {
            visitIdent((AJCIdent) that);
        } else if (that instanceof AJCLiteral) {
            visitLiteral((AJCLiteral) that);
        } else if (that instanceof AJCPrimitiveTypeTree) {
            visitPrimitiveType((AJCPrimitiveTypeTree) that);
        } else if (that instanceof AJCArrayTypeTree) {
            visitArrayType((AJCArrayTypeTree) that);
        } else if (that instanceof AJCTypeApply) {
            visitTypeApply((AJCTypeApply) that);
        } else if (that instanceof AJCTypeUnion) {
            visitTypeUnion((AJCTypeUnion) that);
        } else if (that instanceof AJCTypeIntersection) {
            visitTypeIntersection((AJCTypeIntersection) that);
        } else if (that instanceof AJCTypeParameter) {
            visitTypeParameter((AJCTypeParameter) that);
        } else if (that instanceof AJCWildcard) {
            visitWildcard((AJCWildcard) that);
        } else if (that instanceof AJCTypeBoundKind) {
            visitTypeBoundKind((AJCTypeBoundKind) that);
        } else if (that instanceof AJCAnnotation) {
            visitAnnotation((AJCAnnotation) that);
        } else if (that instanceof AJCModifiers) {
            visitModifiers((AJCModifiers) that);
        } else if (that instanceof AJCAnnotatedType) {
            visitAnnotatedType((AJCAnnotatedType) that);
        } else if (that instanceof AJCLetExpr) {
            visitLetExpr((AJCLetExpr) that);
        } else if (that instanceof AJCEmptyExpression) {
            visitEmptyExpression((AJCEmptyExpression) that);
        } else if (that instanceof AJCErroneous) {
            visitErroneous((AJCErroneous) that);
        }
    }

    public void visit(List<? extends AJCTree> trees) {
        if (trees == null) {
            return;
        }

        for (AJCTree t : trees) {
            visit(t);
        }
    }

    @Override
    public void visitImport(AJCImport that) {
        visit(that.qualid);
    }

    @Override
    public void visitClassDef(AJCClassDecl that) {
        visit(that.mods);
        visit(that.typarams);
        visit(that.extending);
        visit(that.implementing);
        visit(that.fields);
        visit(that.methods);
        visit(that.classes);
    }

    @Override
    public void visitMethodDef(AJCMethodDecl that) {
        visit(that.mods);
        visit(that.restype);
        visit(that.typarams);
        visit(that.recvparam);
        visit(that.params);
        visit(that.thrown);
        visit(that.body);
        visit(that.defaultValue);
    }

    @Override
    public void visitVariableDecl(AJCVariableDecl that) {
        visit(that.mods);
        visit(that.nameexpr);
        visit(that.vartype);
        visit(that.init);
    }

    @Override
    public void visitSkip(AJCSkip that) { };

    @Override
    public void visitEmptyExpression(AJCEmptyExpression that) { };

    @Override
    public void visitBlock(AJCBlock that) {
        visit(that.stats);
    }

    @Override
    public void visitDoWhileLoop(AJCDoWhileLoop that) {
        visit(that.body);
        visit(that.cond);
    }

    @Override
    public void visitWhileLoop(AJCWhileLoop that) {
        visit(that.cond);
        visit(that.body);
    }

    @Override
    public void visitForLoop(AJCForLoop that) {
        visit(that.init);
        visit(that.cond);
        visit(that.step);
        visit(that.body);
    }

    @Override
    public void visitForeachLoop(AJCForEachLoop that) {
        visit(that.var);
        visit(that.expr);
        visit(that.body);
    }

    @Override
    public void visitLabelledStatement(AJCLabeledStatement that) {
        visit(that.body);
    }

    @Override
    public void visitSwitch(AJCSwitch that) {
        visit(that.selector);
        visit(that.cases);
    }

    @Override
    public void visitCase(AJCCase that) {
        visit(that.pat);
        visit(that.stats);
    }

    @Override
    public void visitSynchronized(AJCSynchronized that) {
        visit(that.lock);
        visit(that.body);
    }

    @Override
    public void visitTry(AJCTry that) {
        visit(that.resources);
        visit(that.body);
        visit(that.catchers);
        visit(that.finalizer);
    }

    @Override
    public void visitCatch(AJCCatch that) {
        visit(that.param);
        visit(that.body);
    }

    @Override
    public void visitConditional(AJCConditional that) {
        visit(that.cond);
        visit(that.truepart);
        visit(that.falsepart);
    }

    @Override
    public void visitIf(AJCIf that) {
        visit(that.cond);
        visit(that.thenpart);
        visit(that.elsepart);
    }

    @Override
    public void visitExpressionStatement(AJCExpressionStatement that) {
        visit(that.expr);
    }

    @Override
    public void visitBreak(AJCBreak that) {}

    @Override
    public void visitContinue(AJCContinue that) {}

    @Override
    public void visitReturn(AJCReturn that) {
        visit(that.expr);
    }

    @Override
    public void visitThrow(AJCThrow that) {
        visit(that.expr);
    }

    @Override
    public void visitAssert(AJCAssert that) {
        visit(that.cond);
        visit(that.detail);
    }

    @Override
    public void visitCall(AJCCall that) {
        visit(that.typeargs);
        visit(that.meth);
        visit(that.args);
    }

    @Override
    public void visitNewClass(AJCNewClass that) {
        visit(that.encl);
        visit(that.typeargs);
        visit(that.clazz);
        visit(that.args);
        visit(that.def);
    }

    @Override
    public void visitNewArray(AJCNewArray that) {
        visit(that.annotations);
        for (List<AJCAnnotation> origDimAnnos : that.dimAnnotations) {
            visit(origDimAnnos);
        }
        visit(that.elemtype);
        visit(that.dims);
        visit(that.elems);
    }

    @Override
    public void visitLambda(AJCLambda that) {
        visit(that.params);
        visit(that.body);
    }

    @Override
    public void visitAssign(AJCAssign that) {
        visit(that.lhs);
        visit(that.rhs);
    }

    @Override
    public void visitAssignop(AJCAssignOp that) {
        visit(that.lhs);
        visit(that.rhs);
    }

    @Override
    public void visitUnary(AJCUnary that) {
        visit(that.arg);
    }

    @Override
    public void visitUnaryAsg(AJCUnaryAsg that) {
        visit(that.arg);
    }

    @Override
    public void visitBinary(AJCBinary that) {
        visit(that.lhs);
        visit(that.rhs);
    }

    @Override
    public void visitTypeCast(AJCTypeCast that) {
        visit(that.clazz);
        visit(that.expr);
    }

    @Override
    public void visitInstanceOf(AJCInstanceOf that) {
        visit(that.clazz);
        visit(that.expr);
    }

    @Override
    public void visitArrayAccess(AJCArrayAccess that) {
        visit(that.index);
        visit(that.indexed);
    }

    @Override
    public void visitFieldAccess(AJCFieldAccess that) {
        visit(that.selected);
    }

    @Override
    public void visitMemberReference(AJCMemberReference that) {
        visit(that.typeargs);
        visit(that.expr);
    }

    @Override
    public void visitIdent(AJCIdent that) { }

    @Override
    public void visitLiteral(AJCLiteral that) { }

    @Override
    public void visitPrimitiveType(AJCPrimitiveTypeTree that) { }

    @Override
    public void visitArrayType(AJCArrayTypeTree that) {
        visit(that.elemtype);
    }

    @Override
    public void visitTypeApply(AJCTypeApply that) {
        visit(that.clazz);
        visit(that.arguments);
    }

    @Override
    public void visitTypeUnion(AJCTypeUnion that) {
        visit(that.alternatives);
    }

    @Override
    public void visitTypeIntersection(AJCTypeIntersection that) {
        visit(that.bounds);
    }

    @Override
    public void visitTypeParameter(AJCTypeParameter that) {
        visit(that.annotations);
        visit(that.bounds);
    }

    @Override
    public void visitWildcard(AJCWildcard that) {
        visit(that.kind);
        visit(that.inner);
    }

    @Override
    public void visitTypeBoundKind(AJCTypeBoundKind that) { }

    @Override
    public void visitAnnotation(AJCAnnotation that) {
        visit(that.annotationType);
        visit(that.args);
    }

    @Override
    public void visitModifiers(AJCModifiers that) {
        visit(that.annotations);
    }

    @Override
    public void visitAnnotatedType(AJCAnnotatedType that) {
        visit(that.annotations);
        visit(that.underlyingType);
    }

    @Override
    public void visitErroneous(AJCErroneous that) { }

    @Override
    public void visitLetExpr(AJCLetExpr that) {
        visit(that.defs);
        visit(that.expr);
    }
}