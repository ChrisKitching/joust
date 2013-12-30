package joust.optimisers.visitors;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.treeinfo.EffectSet;
import joust.treeinfo.TreeInfoManager;
import joust.utils.DepthFirstTreeVisitor;
import joust.utils.LogUtils;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.treeinfo.EffectSet.Effects;

public @Log4j2
class SideEffectVisitor extends DepthFirstTreeVisitor {
    /**
     * Compute the union of the EffectSets of the given JCTree elements. Each individual tree must
     * have had its EffectSet registered previously.
     *
     * @param trees Trees to find the union EffectSet for.
     * @return An EffectSet representing the union of the effect sets of the given trees.
     */
    private EffectSet unionNodeEffects(List<? extends JCTree> trees) {
        final int numTrees = trees.size();
        if (numTrees == 0) {
            return EffectSet.getEffectSet(Effects.NONE);
        }

        EffectSet effects = TreeInfoManager.getEffects(trees.get(0));
        for (int i = 1; i < numTrees; i++) {
            effects = effects.union(TreeInfoManager.getEffects(trees.get(i)));
        }

        return effects;
    }

    @Override
    public void visitMethodDef(JCMethodDecl that) {
        super.visitMethodDef(that);

        List<JCExpression> thrownExceptions = that.thrown;
        for (JCExpression e : thrownExceptions) {
            log.debug("Thrown: {} : {}", e, e.getClass());
        }

        EffectSet effects = TreeInfoManager.getEffects(that.body);
        if (!thrownExceptions.isEmpty()) {
            effects = effects.union(Effects.EXCEPTION);
        }

        TreeInfoManager.registerEffects(that, effects);
    }

    @Override
    public void visitSkip(JCSkip that) {
        super.visitSkip(that);

        TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop that) {
        super.visitDoLoop(that);

        // The effect set of a do-while loop is the union of its condition with its body.
        EffectSet condEffects = TreeInfoManager.getEffects(that.cond);
        EffectSet bodyEffects = TreeInfoManager.getEffects(that.body);

        TreeInfoManager.registerEffects(that, condEffects.union(bodyEffects));
    }

    @Override
    public void visitBlock(JCBlock that) {
        super.visitBlock(that);

        TreeInfoManager.registerEffects(that, unionNodeEffects(that.stats));
    }

    @Override
    public void visitWhileLoop(JCWhileLoop that) {
        super.visitWhileLoop(that);

        // The effect set of a while loop is the union of its condition with its body.
        EffectSet condEffects = TreeInfoManager.getEffects(that.cond);
        EffectSet bodyEffects = TreeInfoManager.getEffects(that.body);

        TreeInfoManager.registerEffects(that, condEffects.union(bodyEffects));
    }

    @Override
    public void visitForLoop(JCForLoop that) {
        super.visitForLoop(that);

        // The effect set of a while loop is the union of its condition with its body.
        EffectSet initEffects = unionNodeEffects(that.init);
        EffectSet stepEffects = unionNodeEffects(that.step);
        EffectSet condEffects = TreeInfoManager.getEffects(that.cond);
        EffectSet bodyEffects = TreeInfoManager.getEffects(that.body);

        TreeInfoManager.registerEffects(that, condEffects.union(bodyEffects, initEffects, stepEffects));
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop that) {
        super.visitForeachLoop(that);

        EffectSet exprEffects = TreeInfoManager.getEffects(that.body);
        EffectSet bodyEffects = TreeInfoManager.getEffects(that.expr);

        TreeInfoManager.registerEffects(that, exprEffects.union(bodyEffects));
    }

    @Override
    public void visitLabelled(JCLabeledStatement that) {
        super.visitLabelled(that);
        TreeInfoManager.registerEffects(that, TreeInfoManager.getEffects(that.body));
    }

    @Override
    public void visitSwitch(JCSwitch that) {
        super.visitSwitch(that);
        EffectSet condEffects = TreeInfoManager.getEffects(that.selector);
        EffectSet caseEffects = unionNodeEffects(that.cases);

        TreeInfoManager.registerEffects(that, condEffects.union(caseEffects));
    }

    @Override
    public void visitCase(JCCase that) {
        super.visitCase(that);

        EffectSet condEffects = TreeInfoManager.getEffects(that.pat);
        EffectSet bodyEffects = unionNodeEffects(that.stats);

        TreeInfoManager.registerEffects(that, condEffects.union(bodyEffects));
    }

    @Override
    public void visitSynchronized(JCSynchronized that) {
        super.visitSynchronized(that);

        EffectSet lockEffects = TreeInfoManager.getEffects(that.lock);
        EffectSet bodyEffects = TreeInfoManager.getEffects(that.body);

        TreeInfoManager.registerEffects(that, lockEffects.union(bodyEffects));
    }

    @Override
    public void visitTry(JCTry that) {
        super.visitTry(that);

        // Union ALL the things.
        EffectSet bodyEffects = TreeInfoManager.getEffects(that.body);
        EffectSet catcherEffects = unionNodeEffects(that.catchers);
        EffectSet finalizerEffects = TreeInfoManager.getEffects(that.finalizer);
        EffectSet resourceEffects = unionNodeEffects(that.resources);

        TreeInfoManager.registerEffects(that, bodyEffects.union(catcherEffects, finalizerEffects, resourceEffects));
    }

    @Override
    public void visitCatch(JCCatch that) {
        super.visitCatch(that);
        TreeInfoManager.registerEffects(that, TreeInfoManager.getEffects(that.body));
    }

    @Override
    public void visitConditional(JCConditional that) {
        super.visitConditional(that);

        EffectSet condEffects = TreeInfoManager.getEffects(that.cond);
        EffectSet trueEffects = TreeInfoManager.getEffects(that.truepart);
        EffectSet falseEffects = TreeInfoManager.getEffects(that.falsepart);

        TreeInfoManager.registerEffects(that, condEffects.union(trueEffects, falseEffects));
    }

    @Override
    public void visitIf(JCIf that) {
        super.visitIf(that);

        EffectSet condEffects = TreeInfoManager.getEffects(that.cond);
        EffectSet trueEffects = TreeInfoManager.getEffects(that.thenpart);
        if (that.elsepart != null) {
            EffectSet falseEffects = TreeInfoManager.getEffects(that.elsepart);
            TreeInfoManager.registerEffects(that, condEffects.union(trueEffects, falseEffects));
        }  else {
            TreeInfoManager.registerEffects(that, condEffects.union(trueEffects));
        }
    }

    @Override
    public void visitExec(JCExpressionStatement that) {
        super.visitExec(that);

        TreeInfoManager.registerEffects(that, TreeInfoManager.getEffects(that.expr));
    }

    @Override
    public void visitBreak(JCBreak that) {
        super.visitBreak(that);

        TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitContinue(JCContinue that) {
        super.visitContinue(that);

        TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitReturn(JCReturn that) {
        super.visitReturn(that);

        TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitThrow(JCThrow that) {
        super.visitThrow(that);

        TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.EXCEPTION));
    }

    @Override
    public void visitAssert(JCAssert that) {
        super.visitAssert(that);

        // Hopefully the empty set...
        EffectSet condEffects = TreeInfoManager.getEffects(that.cond);

        TreeInfoManager.registerEffects(that, condEffects.union(EffectSet.getEffectSet(Effects.EXCEPTION)));
    }

    @Override
    public void visitApply(JCMethodInvocation that) {
        super.visitApply(that);

        MethodSymbol methodSym;
        if (that.meth instanceof JCFieldAccess) {
            methodSym = (MethodSymbol) ((JCFieldAccess) that.meth).sym;
        } else if (that.meth instanceof JCIdent) {
            methodSym = (MethodSymbol) ((JCIdent) that.meth).sym;
        } else {
            LogUtils.raiseCompilerError("Unexpected application type: " + that.meth.getClass().getName() + " for node: " + that);
            return;
        }

        EffectSet methodEffects = TreeInfoManager.getEffectsForMethod(methodSym);

        // Read/write to locals within the method called do not apply in this scope.
        methodEffects = methodEffects.subtract(EffectSet.getEffectSet(Effects.READ_LOCAL, Effects.WRITE_LOCAL));
        EffectSet argEffects = unionNodeEffects(that.args);

        TreeInfoManager.registerEffects(that, methodEffects.union(argEffects));
    }

    @Override
    public void visitNewClass(JCNewClass that) {
        super.visitNewClass(that);

        // Effects of the args plus effects of the constructor.
        EffectSet argEffects = unionNodeEffects(that.args);
        EffectSet methodEffects = TreeInfoManager.getEffectsForMethod((MethodSymbol) that.constructor);

        TreeInfoManager.registerEffects(that, argEffects.union(methodEffects));
    }

    @Override
    public void visitNewArray(JCNewArray that) {
        super.visitNewArray(that);

        // A new array operation *by itself* has no side effects (Neglecting ever-present possibilities
        // such as OutOfMemoryException. It is the corresponding assignment (If any exists) which
        // has a side effect.
        // Of course, the argument to the new array call might have side effects, as might the
        // elements of the array (If given explicitly).
        EffectSet elementEffects = unionNodeEffects(that.elems);
        EffectSet dimensionEffects = unionNodeEffects(that.dims);

        TreeInfoManager.registerEffects(that, elementEffects.union(dimensionEffects));
    }

    @Override
    public void visitParens(JCParens that) {
        super.visitParens(that);

        TreeInfoManager.registerEffects(that, TreeInfoManager.getEffects(that.expr));
    }

    @Override
    public void visitAssign(JCAssign that) {
        super.visitAssign(that);

        EffectSet rhsEffects = TreeInfoManager.getEffects(that.rhs);

        VarSymbol varSym;
        if (that.lhs instanceof JCFieldAccess) {
            varSym = (VarSymbol) ((JCFieldAccess) that.lhs).sym;
        } else if (that.lhs instanceof JCIdent) {
            varSym = (VarSymbol) ((JCIdent) that.lhs).sym;
        } else {
            LogUtils.raiseCompilerError("Unexpected assignment type: " + that.lhs.getClass().getName() + " for node: " + that);
            return;
        }

        if (varSym.owner instanceof MethodSymbol) {
            TreeInfoManager.registerEffects(that, rhsEffects.union(EffectSet.getEffectSet(Effects.WRITE_LOCAL)));
        } else if (varSym.owner instanceof ClassSymbol) {
            TreeInfoManager.registerEffects(that, rhsEffects.union(EffectSet.getEffectSet(Effects.WRITE_GLOBAL)));
        }
    }

    @Override
    public void visitAssignop(JCAssignOp that) {
        super.visitAssignop(that);

        // Assignment with an operator, such as +=. Same side effect profile as visitAssign?
        EffectSet rhsEffects = TreeInfoManager.getEffects(that.rhs);

        // LHS is presumably a VarSymbol...
        VarSymbol varSym = (VarSymbol) ((JCIdent) that.lhs).sym;

        if (TreeUtils.isLocalVariable(varSym)) {
            TreeInfoManager.registerEffects(that, rhsEffects.union(EffectSet.getEffectSet(Effects.WRITE_LOCAL, Effects.READ_LOCAL)));
        } else if (varSym.owner instanceof ClassSymbol) {
            TreeInfoManager.registerEffects(that, rhsEffects.union(EffectSet.getEffectSet(Effects.WRITE_GLOBAL, Effects.WRITE_GLOBAL)));
        }
    }

    @Override
    public void visitUnary(JCUnary that) {
        super.visitUnary(that);

        EffectSet argEffects = TreeInfoManager.getEffects(that.arg);

        // If the operation is a ++ or --, it has write side effects on the target. Otherwise, it has
        // no side effects.
        final Tag nodeTag = that.getTag();
        if (nodeTag == Tag.PREINC
         || nodeTag == Tag.PREDEC
         || nodeTag == Tag.POSTINC
         || nodeTag == Tag.POSTDEC) {
            VarSymbol varSym = null;

            // It is irritating how little javac uses its own type system...
            if (that.arg instanceof JCIdent) {
                varSym = (VarSymbol) ((JCIdent) that.arg).sym;
            } else if (that.arg instanceof JCFieldAccess) {
                varSym = (VarSymbol) ((JCFieldAccess) that.arg).sym;
            }

            if (TreeUtils.isLocalVariable(varSym)) {
                TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.WRITE_LOCAL, Effects.READ_LOCAL));
            } else {
                TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.WRITE_GLOBAL, Effects.READ_GLOBAL));
            }
        } else {
            TreeInfoManager.registerEffects(that, argEffects);
        }
    }

    @Override
    public void visitBinary(JCBinary that) {
        super.visitBinary(that);

        EffectSet lhsEffects = TreeInfoManager.getEffects(that.lhs);
        EffectSet rhsEffects = TreeInfoManager.getEffects(that.rhs);

        TreeInfoManager.registerEffects(that, lhsEffects.union(rhsEffects));
    }

    @Override
    public void visitIndexed(JCArrayAccess that) {
        super.visitIndexed(that);

        EffectSet nodeEffects = TreeInfoManager.getEffects(that.index);

        if (TreeUtils.isLocalVariable(that.indexed)) {
            nodeEffects.union(Effects.READ_LOCAL);
        } else {
            nodeEffects.union(Effects.READ_GLOBAL);
        }

        TreeInfoManager.registerEffects(that, nodeEffects);
    }

    @Override
    public void visitSelect(JCFieldAccess that) {
        super.visitSelect(that);

        EffectSet argEffects = TreeInfoManager.getEffects(that.selected);

        TreeInfoManager.registerEffects(that, argEffects.union(EffectSet.getEffectSet(Effects.READ_GLOBAL)));
    }

    @Override
    public void visitIdent(JCIdent that) {
        super.visitIdent(that);

        Symbol sym = that.sym;

        if (sym instanceof VarSymbol) {
            if (TreeUtils.isLocalVariable(sym)) {
                TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.READ_LOCAL));
            } else {
                TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.READ_GLOBAL));
            }

            return;
        }

        TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitLiteral(JCLiteral that) {
        super.visitLiteral(that);
        TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitErroneous(JCErroneous that) {
        super.visitErroneous(that);

        log.error("Encountered errorneous tree: {}", that);
        TreeInfoManager.registerEffects(that, EffectSet.getEffectSet(Effects.getAllEffects()));
    }

    @Override
    public void visitLetExpr(LetExpr that) {
        super.visitLetExpr(that);

        // Hopefully the empty set...
        EffectSet defEffects = unionNodeEffects(that.defs);
        EffectSet condEffects = TreeInfoManager.getEffects(that.expr);

        TreeInfoManager.registerEffects(that, defEffects.union(condEffects));
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        super.visitTypeCast(jcTypeCast);
        TreeInfoManager.registerEffects(jcTypeCast, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        super.visitTypeTest(jcInstanceOf);
        TreeInfoManager.registerEffects(jcInstanceOf, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitReference(JCMemberReference jcMemberReference) {
        super.visitReference(jcMemberReference);
        Symbol referenced = jcMemberReference.sym;

        EffectSet exprEffects = TreeInfoManager.getEffects(jcMemberReference.expr);

        // TODO: Determine if the access is to a local object.
        if (referenced instanceof VarSymbol) {
            TreeInfoManager.registerEffects(jcMemberReference, exprEffects.union(EffectSet.getEffectSet(Effects.READ_GLOBAL)));
        } else {
            // It could be a reference to a method, for example. These have no side-effects. (But the
            // associated call might.)
            TreeInfoManager.registerEffects(jcMemberReference, exprEffects.union(EffectSet.getEffectSet(Effects.NONE)));
        }
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        super.visitClassDef(jcClassDecl);
        TreeInfoManager.registerEffects(jcClassDecl, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        super.visitVarDef(jcVariableDecl);
        EffectSet initEffects;

        if (jcVariableDecl.init != null) {
            initEffects = TreeInfoManager.getEffects(jcVariableDecl.init);
        } else {
            initEffects = EffectSet.getEffectSet(Effects.NONE);
        }

        if (TreeUtils.isLocalVariable(jcVariableDecl.sym)) {
            TreeInfoManager.registerEffects(jcVariableDecl, initEffects.union(EffectSet.getEffectSet(Effects.WRITE_LOCAL)));
        } else {
            TreeInfoManager.registerEffects(jcVariableDecl, initEffects.union(EffectSet.getEffectSet(Effects.WRITE_GLOBAL)));
        }
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        super.visitTypeIdent(jcPrimitiveTypeTree);
        TreeInfoManager.registerEffects(jcPrimitiveTypeTree, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree jcArrayTypeTree) {
        super.visitTypeArray(jcArrayTypeTree);
        TreeInfoManager.registerEffects(jcArrayTypeTree, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitTypeApply(JCTypeApply jcTypeApply) {
        super.visitTypeApply(jcTypeApply);
        TreeInfoManager.registerEffects(jcTypeApply, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitTypeUnion(JCTypeUnion jcTypeUnion) {
        super.visitTypeUnion(jcTypeUnion);
        TreeInfoManager.registerEffects(jcTypeUnion, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitTypeIntersection(JCTypeIntersection jcTypeIntersection) {
        super.visitTypeIntersection(jcTypeIntersection);
        TreeInfoManager.registerEffects(jcTypeIntersection, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitTypeParameter(JCTypeParameter jcTypeParameter) {
        super.visitTypeParameter(jcTypeParameter);
        TreeInfoManager.registerEffects(jcTypeParameter, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind typeBoundKind) {
        super.visitTypeBoundKind(typeBoundKind);
        TreeInfoManager.registerEffects(typeBoundKind, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitWildcard(JCWildcard jcWildcard) {
        super.visitWildcard(jcWildcard);
        TreeInfoManager.registerEffects(jcWildcard, EffectSet.getEffectSet(Effects.NONE));
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        super.visitAnnotation(jcAnnotation);
        TreeInfoManager.registerEffects(jcAnnotation, EffectSet.getEffectSet(Effects.NONE));
    }
}