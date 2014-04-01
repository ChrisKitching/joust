package joust.tree.annotatedtree;

import com.sun.tools.javac.util.List;

import static joust.tree.annotatedtree.AJCTree.*;

/**
 * The default implementation of the tree visitor interface. Traverses the tree in a depth-first manner.
 * Differs notably from javac's tree traversers in that it doesn't put the traversal logic in the tree nodes
 * themselves.
 */
public abstract class AJCTreeVisitor {
    /**
     * The entry point for users of the class...
     * @param tree
     */
    public void visitTree(AJCTree tree) {
        visit(tree);
    }
    public void visitTrees(List<? extends AJCTree> trees) {
        visit(trees);
    }

    // TODO: Something less stupid than this.
    protected void visit(AJCTree that) {
        if (that == null) {
            return;
        }

        if (that instanceof AJCClassDecl) {
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
        } else if (that instanceof AJCCall) {
            visitCall((AJCCall) that);
        } else if (that instanceof AJCNewClass) {
            visitNewClass((AJCNewClass) that);
        } else if (that instanceof AJCNewArray) {
            visitNewArray((AJCNewArray) that);
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
        } else if (that instanceof AJCIdent) {
            visitIdent((AJCIdent) that);
        } else if (that instanceof AJCLiteral) {
            visitLiteral((AJCLiteral) that);
        } else if (that instanceof AJCPrimitiveTypeTree) {
            visitPrimitiveType((AJCPrimitiveTypeTree) that);
        } else if (that instanceof AJCArrayTypeTree) {
            visitArrayType((AJCArrayTypeTree) that);
        } else if (that instanceof AJCTypeUnion) {
            visitTypeUnion((AJCTypeUnion) that);
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

    protected void visit(List<? extends AJCTree> trees) {
        if (trees == null) {
            return;
        }

        for (AJCTree t : trees) {
            visit(t);
        }
    }

    protected void visitClassDef(AJCClassDecl that) {
        visit(that.mods);
        visit(that.extending);
        visit(that.implementing);
        visit(that.fields);
        visit(that.methods);
        visit(that.classes);
    }

    protected void visitMethodDef(AJCMethodDecl that) {
        visit(that.restype);
        visit(that.mods);
        visit(that.recvparam);
        visit(that.params);
        visit(that.thrown);
        visit(that.body);
        visit(that.defaultValue);
    }

    protected void visitVariableDecl(AJCVariableDecl that) {
        visit(that.mods);
        visit(that.init);
        visit(that.vartype);
    }

    protected void visitSkip(AJCSkip that) {};

    protected void visitEmptyExpression(AJCEmptyExpression that) {};

    protected void visitBlock(AJCBlock that) {
        visit(that.stats);
    }

    protected void visitDoWhileLoop(AJCDoWhileLoop that) {
        visit(that.body);
        visit(that.cond);
    }

    protected void visitWhileLoop(AJCWhileLoop that) {
        visit(that.cond);
        visit(that.body);
    }

    protected void visitForLoop(AJCForLoop that) {
        visit(that.init);
        visit(that.cond);
        visit(that.step);
        visit(that.body);
    }

    protected void visitLabelledStatement(AJCLabeledStatement that) {
        visit(that.body);
    }

    protected void visitSwitch(AJCSwitch that) {
        visit(that.selector);
        visit(that.cases);
    }

    protected void visitCase(AJCCase that) {
        visit(that.pat);
        visit(that.stats);
    }

    protected void visitSynchronized(AJCSynchronized that) {
        visit(that.lock);
        visit(that.body);
    }

    protected void visitTry(AJCTry that) {
        visit(that.body);
        visit(that.catchers);
        visit(that.finalizer);
    }

    protected void visitCatch(AJCCatch that) {
        visit(that.param);
        visit(that.body);
    }

    protected void visitConditional(AJCConditional that) {
        visit(that.cond);
        visit(that.truepart);
        visit(that.falsepart);
    }

    protected void visitIf(AJCIf that) {
        visit(that.cond);
        visit(that.thenpart);
        visit(that.elsepart);
    }

    protected void visitExpressionStatement(AJCExpressionStatement that) {
        visit(that.expr);
    }

    protected void visitBreak(AJCBreak that) {}

    protected void visitContinue(AJCContinue that) {}

    protected void visitReturn(AJCReturn that) {
        visit(that.expr);
    }

    protected void visitThrow(AJCThrow that) {
        visit(that.expr);
    }

    protected void visitCall(AJCCall that) {
        visit(that.meth);
        visit(that.args);
    }

    protected void visitNewClass(AJCNewClass that) {
        visit(that.clazz);
        visit(that.args);
        visit(that.def);
    }

    protected void visitNewArray(AJCNewArray that) {
        visit(that.annotations);
        visit(that.elemtype);
        visit(that.dims);
        visit(that.elems);
    }

    protected void visitAssign(AJCAssign that) {
        visit(that.lhs);
        visit(that.rhs);
    }

    protected void visitAssignop(AJCAssignOp that) {
        visit(that.lhs);
        visit(that.rhs);
    }

    protected void visitUnary(AJCUnary that) {
        visit(that.arg);
    }

    protected void visitUnaryAsg(AJCUnaryAsg that) {
        visit(that.arg);
    }

    protected void visitBinary(AJCBinary that) {
        visit(that.lhs);
        visit(that.rhs);
    }

    protected void visitTypeCast(AJCTypeCast that) {
        visit(that.clazz);
        visit(that.expr);
    }

    protected void visitInstanceOf(AJCInstanceOf that) {
        visit((AJCTree) that.clazz);
        visit(that.expr);
    }

    protected void visitArrayAccess(AJCArrayAccess that) {
        visit(that.index);
        visit(that.indexed);
    }

    protected void visitFieldAccess(AJCFieldAccess that) {
        visit(that.selected);
    }

    protected void visitIdent(AJCIdent that) { }

    protected void visitLiteral(AJCLiteral that) { }

    protected void visitPrimitiveType(AJCPrimitiveTypeTree that) { }

    protected void visitArrayType(AJCArrayTypeTree that) {
        visit(that.elemtype);
    }

    protected void visitTypeUnion(AJCTypeUnion that) {
        visit(that.alternatives);
    }

    protected void visitAnnotation(AJCAnnotation that) {
        // We don't care, and these are sort of complicated.
        //visit(that.annotationType);
        //visit(that.args);
    }

    protected void visitModifiers(AJCModifiers that) {
        visit(that.annotations);
    }

    protected void visitAnnotatedType(AJCAnnotatedType that) {
        visit(that.underlyingType);
    }

    protected void visitErroneous(AJCErroneous that) { }

    protected void visitLetExpr(AJCLetExpr that) {
        visit(that.defs);
        visit(that.expr);
    }
}