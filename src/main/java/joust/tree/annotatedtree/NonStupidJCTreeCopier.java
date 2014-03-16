package joust.tree.annotatedtree;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;

import static com.sun.tools.javac.tree.JCTree.*;
/**
 * A tree copier that actually *copies* the fucking *tree*, instead of javac's one which copies most of the tree,
 * some of the time.
 * Notably, this copier preserves type and symbol annotations, which javac's one does not.
 */
public class NonStupidJCTreeCopier<P> extends TreeCopier<P> {
    /**
     * Creates a new instance of TreeCopier
     */
    public NonStupidJCTreeCopier(TreeMaker M) {
        super(M);
    }

    @Override
    public JCTree visitIdentifier(IdentifierTree node, P p) {
        JCIdent ret = (JCIdent) super.visitIdentifier(node, p);
        JCIdent castInput = (JCIdent) node;

        // Now for the things TreeCopier "forgot"...
        ret.sym = castInput.sym;
        ret.type = castInput.type;

        return ret;
    }

    @Override
    public JCTree visitBinary(BinaryTree node, P p) {
        JCBinary ret = (JCBinary) super.visitBinary(node, p);
        JCBinary castInput = (JCBinary) node;

        ret.operator = castInput.operator;
        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitUnary(UnaryTree node, P p) {
        JCUnary ret = (JCUnary) super.visitUnary(node, p);
        JCUnary castInput = (JCUnary) node;

        ret.operator = castInput.operator;
        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        JCAssignOp ret = (JCAssignOp) super.visitCompoundAssignment(node, p);
        JCAssignOp castInput = (JCAssignOp) node;

        ret.operator = castInput.operator;
        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitMemberSelect(MemberSelectTree node, P p) {
        JCFieldAccess ret = (JCFieldAccess) super.visitMemberSelect(node, p);
        JCFieldAccess castInput = (JCFieldAccess) node;

        ret.sym = castInput.sym;
        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitAnnotatedType(AnnotatedTypeTree node, P p) {
        JCAnnotatedType ret = (JCAnnotatedType) super.visitAnnotatedType(node, p);
        JCAnnotatedType castInput = (JCAnnotatedType) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitAnnotation(AnnotationTree node, P p) {
        JCAnnotation ret = (JCAnnotation) super.visitAnnotation(node, p);
        JCAnnotation castInput = (JCAnnotation) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitAssert(AssertTree node, P p) {
        JCAssert ret = (JCAssert) super.visitAssert(node, p);
        JCAssert castInput = (JCAssert) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitAssignment(AssignmentTree node, P p) {
        JCAssign ret = (JCAssign) super.visitAssignment(node, p);
        JCAssign castInput = (JCAssign) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitBlock(BlockTree node, P p) {
        JCBlock ret = (JCBlock) super.visitBlock(node, p);
        JCBlock castInput = (JCBlock) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitBreak(BreakTree node, P p) {
        JCBreak ret = (JCBreak) super.visitBreak(node, p);
        JCBreak castInput = (JCBreak) node;

        ret.type = castInput.type;
        ret.target = castInput.target;
        return ret;
    }

    @Override
    public JCTree visitCase(CaseTree node, P p) {
        JCCase ret = (JCCase) super.visitCase(node, p);
        JCCase castInput = (JCCase) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitCatch(CatchTree node, P p) {
        JCCatch ret = (JCCatch) super.visitCatch(node, p);
        JCCatch castInput = (JCCatch) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitClass(ClassTree node, P p) {
        JCClassDecl ret = (JCClassDecl) super.visitClass(node, p);
        JCClassDecl castInput = (JCClassDecl) node;

        ret.type = castInput.type;
        ret.sym = castInput.sym;
        return ret;
    }

    @Override
    public JCTree visitConditionalExpression(ConditionalExpressionTree node, P p) {
        JCConditional ret = (JCConditional) super.visitConditionalExpression(node, p);
        JCConditional castInput = (JCConditional) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitContinue(ContinueTree node, P p) {
        JCContinue ret = (JCContinue) super.visitContinue(node, p);
        JCContinue castInput = (JCContinue) node;

        ret.type = castInput.type;
        ret.target = castInput.target;
        return ret;
    }

    @Override
    public JCTree visitDoWhileLoop(DoWhileLoopTree node, P p) {
        JCDoWhileLoop ret = (JCDoWhileLoop) super.visitDoWhileLoop(node, p);
        JCDoWhileLoop castInput = (JCDoWhileLoop) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitErroneous(ErroneousTree node, P p) {
        return super.visitErroneous(node, p);
    }

    @Override
    public JCTree visitExpressionStatement(ExpressionStatementTree node, P p) {
        JCExpressionStatement ret = (JCExpressionStatement) super.visitExpressionStatement(node, p);
        JCExpressionStatement castInput = (JCExpressionStatement) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        JCEnhancedForLoop ret = (JCEnhancedForLoop) super.visitEnhancedForLoop(node, p);
        JCEnhancedForLoop castInput = (JCEnhancedForLoop) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitForLoop(ForLoopTree node, P p) {
        JCForLoop ret = (JCForLoop) super.visitForLoop(node, p);
        JCForLoop castInput = (JCForLoop) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitIf(IfTree node, P p) {
        JCIf ret = (JCIf) super.visitIf(node, p);
        JCIf castInput = (JCIf) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitImport(ImportTree node, P p) {
        JCImport ret = (JCImport) super.visitImport(node, p);
        JCImport castInput = (JCImport) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitArrayAccess(ArrayAccessTree node, P p) {
        JCArrayAccess ret = (JCArrayAccess) super.visitArrayAccess(node, p);
        JCArrayAccess castInput = (JCArrayAccess) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitLabeledStatement(LabeledStatementTree node, P p) {
        JCLabeledStatement ret = (JCLabeledStatement) super.visitLabeledStatement(node, p);
        JCLabeledStatement castInput = (JCLabeledStatement) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitLiteral(LiteralTree node, P p) {
        JCLiteral ret = (JCLiteral) super.visitLiteral(node, p);
        JCLiteral castInput = (JCLiteral) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitMethod(MethodTree node, P p) {
        JCMethodDecl ret = (JCMethodDecl) super.visitMethod(node, p);
        JCMethodDecl castInput = (JCMethodDecl) node;

        ret.type = castInput.type;
        ret.sym = castInput.sym;
        return ret;
    }

    @Override
    public JCTree visitMethodInvocation(MethodInvocationTree node, P p) {
        JCMethodInvocation ret = (JCMethodInvocation) super.visitMethodInvocation(node, p);
        JCMethodInvocation castInput = (JCMethodInvocation) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitModifiers(ModifiersTree node, P p) {
        JCModifiers ret = (JCModifiers) super.visitModifiers(node, p);
        JCModifiers castInput = (JCModifiers) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitNewArray(NewArrayTree node, P p) {
        JCNewArray ret = (JCNewArray) super.visitNewArray(node, p);
        JCNewArray castInput = (JCNewArray) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitNewClass(NewClassTree node, P p) {
        JCNewClass ret = (JCNewClass) super.visitNewClass(node, p);
        JCNewClass castInput = (JCNewClass) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitLambdaExpression(LambdaExpressionTree node, P p) {
        JCLambda ret = (JCLambda) super.visitLambdaExpression(node, p);
        JCLambda castInput = (JCLambda) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitParenthesized(ParenthesizedTree node, P p) {
        JCParens ret = (JCParens) super.visitParenthesized(node, p);
        JCParens castInput = (JCParens) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitReturn(ReturnTree node, P p) {
        JCReturn ret = (JCReturn) super.visitReturn(node, p);
        JCReturn castInput = (JCReturn) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitMemberReference(MemberReferenceTree node, P p) {
        JCMemberReference ret = (JCMemberReference) super.visitMemberReference(node, p);
        JCMemberReference castInput = (JCMemberReference) node;

        ret.type = castInput.type;
        ret.sym = castInput.sym;
        return ret;
    }

    @Override
    public JCTree visitEmptyStatement(EmptyStatementTree node, P p) {
        JCSkip ret = (JCSkip) super.visitEmptyStatement(node, p);
        JCSkip castInput = (JCSkip) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitSwitch(SwitchTree node, P p) {
        JCSwitch ret = (JCSwitch) super.visitSwitch(node, p);
        JCSwitch castInput = (JCSwitch) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitSynchronized(SynchronizedTree node, P p) {
        JCSynchronized ret = (JCSynchronized) super.visitSynchronized(node, p);
        JCSynchronized castInput = (JCSynchronized) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitThrow(ThrowTree node, P p) {
        JCThrow ret = (JCThrow) super.visitThrow(node, p);
        JCThrow castInput = (JCThrow) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitCompilationUnit(CompilationUnitTree node, P p) {
        JCCompilationUnit ret = (JCCompilationUnit) super.visitCompilationUnit(node, p);
        JCCompilationUnit castInput = (JCCompilationUnit) node;

        ret.type = castInput.type;
        ret.sourcefile = castInput.sourcefile;
        ret.packge = castInput.packge;
        ret.namedImportScope = castInput.namedImportScope;
        ret.starImportScope = castInput.starImportScope;
        return ret;
    }

    @Override
    public JCTree visitTry(TryTree node, P p) {
        JCTry ret = (JCTry) super.visitTry(node, p);
        JCTry castInput = (JCTry) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitParameterizedType(ParameterizedTypeTree node, P p) {
        JCTypeApply ret = (JCTypeApply) super.visitParameterizedType(node, p);
        JCTypeApply castInput = (JCTypeApply) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitUnionType(UnionTypeTree node, P p) {
        JCTypeUnion ret = (JCTypeUnion) super.visitUnionType(node, p);
        JCTypeUnion castInput = (JCTypeUnion) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitIntersectionType(IntersectionTypeTree node, P p) {
        JCTypeIntersection ret = (JCTypeIntersection) super.visitIntersectionType(node, p);
        JCTypeIntersection castInput = (JCTypeIntersection) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitArrayType(ArrayTypeTree node, P p) {
        JCArrayTypeTree ret = (JCArrayTypeTree) super.visitArrayType(node, p);
        JCArrayTypeTree castInput = (JCArrayTypeTree) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitTypeCast(TypeCastTree node, P p) {
        JCTypeCast ret = (JCTypeCast) super.visitTypeCast(node, p);
        JCTypeCast castInput = (JCTypeCast) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitPrimitiveType(PrimitiveTypeTree node, P p) {
        JCPrimitiveTypeTree ret = (JCPrimitiveTypeTree) super.visitPrimitiveType(node, p);
        JCPrimitiveTypeTree castInput = (JCPrimitiveTypeTree) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitTypeParameter(TypeParameterTree node, P p) {
        JCTypeParameter ret = (JCTypeParameter) super.visitTypeParameter(node, p);
        JCTypeParameter castInput = (JCTypeParameter) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitInstanceOf(InstanceOfTree node, P p) {
        JCInstanceOf ret = (JCInstanceOf) super.visitInstanceOf(node, p);
        JCInstanceOf castInput = (JCInstanceOf) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitVariable(VariableTree node, P p) {
        JCVariableDecl ret = (JCVariableDecl) super.visitVariable(node, p);
        JCVariableDecl castInput = (JCVariableDecl) node;

        ret.type = castInput.type;
        ret.sym = castInput.sym;
        return ret;
    }

    @Override
    public JCTree visitWhileLoop(WhileLoopTree node, P p) {
        JCWhileLoop ret = (JCWhileLoop) super.visitWhileLoop(node, p);
        JCWhileLoop castInput = (JCWhileLoop) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitWildcard(WildcardTree node, P p) {
        JCWildcard ret = (JCWildcard) super.visitWildcard(node, p);
        JCWildcard castInput = (JCWildcard) node;

        ret.type = castInput.type;
        return ret;
    }

    @Override
    public JCTree visitOther(Tree node, P p) {
        LetExpr ret = (LetExpr) super.visitOther(node, p);
        LetExpr castInput = (LetExpr) node;

        ret.type = castInput.type;
        return ret;
    }
}
