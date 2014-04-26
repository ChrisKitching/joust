package joust.tree.annotatedtree;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.utils.logging.LogUtils;
import lombok.AllArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;

/**
 * Wrapper classes for the AJCTree nodes that implement a different scheme for equality testing than the regular ones.
 *
 * The semantics of equals on an AJCExpressionTree are that
 * it returns true if the other expression is equivalent to this one (ie. One node could be replaced with the
 * other in the tree without any affect on the output).
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@AllArgsConstructor
public class AJCComparableExpressionTree<T extends AJCTree & AJCExpression> {
    public final T wrappedNode;

    @Override
    public String toString() {
        return wrappedNode.toString();
    }

    @Override
    public int hashCode() {
        return wrappedNode.toString().hashCode();
    }

    public List<AJCComparableExpressionTree> wrap(List<? extends AJCExpression> that) {
        List<AJCComparableExpressionTree> ret = List.nil();

        for(AJCExpression expr : that) {
            ret = ret.prepend(wrap(expr));
        }

        return ret.reverse();
    }

    public static AJCComparableExpressionTree wrap(AJCExpression that) {
        if (that instanceof AJCConditional) {
            return new ComparableAJCConditional((AJCConditional) that);
        } else if (that instanceof AJCCall) {
            return new ComparableAJCCall((AJCCall) that);
        } else if (that instanceof AJCNewClass) {
            return new ComparableAJCNewClass((AJCNewClass) that);
        } else if (that instanceof AJCNewArray) {
            return new ComparableAJCNewArray((AJCNewArray) that);
        } else if (that instanceof AJCAssign) {
            return new ComparableAJCAssign((AJCAssign) that);
        } else if (that instanceof AJCAssignOp) {
            return new ComparableAJCAssignOp((AJCAssignOp) that);
        } else if (that instanceof AJCUnary) {
            return new ComparableAJCUnary((AJCUnary) that);
        } else if (that instanceof AJCUnaryAsg) {
            return new ComparableAJCUnaryAsg((AJCUnaryAsg) that);
        } else if (that instanceof AJCBinary) {
            return new ComparableAJCBinary((AJCBinary) that);
        } else if (that instanceof AJCTypeCast) {
            return new ComparableAJCTypeCast((AJCTypeCast) that);
        } else if (that instanceof AJCInstanceOf) {
            return new ComparableAJCInstanceOf((AJCInstanceOf) that);
        } else if (that instanceof AJCArrayAccess) {
            return new ComparableAJCArrayAccess((AJCArrayAccess) that);
        } else if (that instanceof AJCLiteral) {
            return new ComparableAJCLiteral((AJCLiteral) that);
        } else if (that instanceof AJCEmptyExpression) {
            return new ComparableAJCEmptyExpression((AJCEmptyExpression) that);
        } else if (that instanceof AJCPrimitiveTypeTree) {
            return new ComparableAJCPrimitiveTypeTree((AJCPrimitiveTypeTree) that);
        }  else if (that instanceof AJCArrayTypeTree) {
            return new ComparableAJCArrayTypeTree((AJCArrayTypeTree) that);
        }  else if (that instanceof AJCTypeUnion) {
            return new ComparableAJCTypeUnion((AJCTypeUnion) that);
        } else if (that instanceof AJCSymbolRefTree) {
            return new ComparableAJCSymbolRefTree((AJCSymbolRefTree) that);
        }

        throw new IllegalArgumentException("Unexpected node type for comparism: " + that + ':' + that.getClass().getCanonicalName());
    }

    public static class ComparableAJCConditional extends AJCComparableExpressionTree<AJCConditional> {
        public ComparableAJCConditional(AJCConditional wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCConditional)) {
                return false;
            }

            ComparableAJCConditional cast = (ComparableAJCConditional) obj;

            return wrap(wrappedNode.cond).equals(wrap(cast.wrappedNode.cond))
                && wrap(wrappedNode.truepart).equals(wrap(cast.wrappedNode.truepart))
                && wrap(wrappedNode.falsepart).equals(wrap(cast.wrappedNode.falsepart));
        }
    }

    public static class ComparableAJCCall extends AJCComparableExpressionTree<AJCCall> {
        public ComparableAJCCall(AJCCall wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCCall)) {
                return false;
            }

            ComparableAJCCall cast = (ComparableAJCCall) obj;

            return wrap(wrappedNode.meth).equals(wrap(cast.wrappedNode.meth))
                && wrap(wrappedNode.args).equals(wrap(cast.wrappedNode.args));
        }
    }

    public static class ComparableAJCNewClass extends AJCComparableExpressionTree<AJCNewClass> {
        public ComparableAJCNewClass(AJCNewClass wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCNewClass)) {
                return false;
            }

            ComparableAJCNewClass cast = (ComparableAJCNewClass) obj;

            return wrappedNode.clazz.getTargetSymbol().equals(cast.wrappedNode.clazz.getTargetSymbol())
                && wrap(wrappedNode.args).equals(wrap(cast.wrappedNode.args))
                && wrappedNode.def == cast.wrappedNode.def;

        }
    }

    public static class ComparableAJCEmptyExpression extends AJCComparableExpressionTree<AJCEmptyExpression> {
        public ComparableAJCEmptyExpression(AJCEmptyExpression wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AJCEmptyExpression;
        }
    }

    public static class ComparableAJCNewArray extends AJCComparableExpressionTree<AJCNewArray> {
        public ComparableAJCNewArray(AJCNewArray wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCNewArray)) {
                return false;
            }

            ComparableAJCNewArray cast = (ComparableAJCNewArray) obj;

            if (!wrap(wrappedNode.elemtype).equals(wrap(cast.wrappedNode.elemtype))) {
                return false;
            }

            return wrap(wrappedNode.dims).equals(wrap(cast.wrappedNode.dims))
                && wrap(wrappedNode.elems).equals(wrap(cast.wrappedNode.elems));
        }
    }

    public static class ComparableAJCAssign extends AJCComparableExpressionTree<AJCAssign> {
        public ComparableAJCAssign(AJCAssign wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCAssign)) {
                return false;
            }

            ComparableAJCAssign cast = (ComparableAJCAssign) obj;
            return wrappedNode.lhs.getTargetSymbol().equals(cast.wrappedNode.lhs.getTargetSymbol())
                && wrap(wrappedNode.rhs).equals(wrap(cast.wrappedNode.rhs));
        }
    }

    public static class ComparableAJCAssignOp extends AJCComparableExpressionTree<AJCAssignOp> {
        public ComparableAJCAssignOp(AJCAssignOp wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCAssignOp)) {
                return false;
            }

            ComparableAJCAssignOp cast = (ComparableAJCAssignOp) obj;

            return wrappedNode.getDecoratedTree().operator.equals(cast.wrappedNode.getDecoratedTree().operator)
                && wrappedNode.lhs.getTargetSymbol().equals(cast.wrappedNode.lhs.getTargetSymbol())
                && wrap(wrappedNode.rhs).equals(wrap(cast.wrappedNode.rhs));
        }
    }

    public static class ComparableAJCUnary extends AJCComparableExpressionTree<AJCUnary> {
        public ComparableAJCUnary(AJCUnary wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCUnary)) {
                return false;
            }

            ComparableAJCUnary cast = (ComparableAJCUnary) obj;

            return wrappedNode.getDecoratedTree().operator.equals(cast.wrappedNode.getDecoratedTree().operator)
                && wrap(wrappedNode.arg).equals(wrap(cast.wrappedNode.arg));
        }
    }

    public static class ComparableAJCUnaryAsg extends AJCComparableExpressionTree<AJCUnaryAsg> {
        public ComparableAJCUnaryAsg(AJCUnaryAsg wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCUnaryAsg)) {
                return false;
            }

            ComparableAJCUnaryAsg cast = (ComparableAJCUnaryAsg) obj;

            return wrappedNode.getDecoratedTree().operator.equals(cast.wrappedNode.getDecoratedTree().operator)
                && wrappedNode.arg.getTargetSymbol().equals(cast.wrappedNode.arg.getTargetSymbol());
        }
    }

    public static class ComparableAJCBinary extends AJCComparableExpressionTree<AJCBinary> {
        public ComparableAJCBinary(AJCBinary wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCBinary)) {
                return false;
            }

            ComparableAJCBinary cast = (ComparableAJCBinary) obj;

            return wrappedNode.getDecoratedTree().operator == cast.wrappedNode.getDecoratedTree().operator
                && wrap(wrappedNode.lhs).equals(wrap(cast.wrappedNode.lhs))
                && wrap(wrappedNode.rhs).equals(wrap(cast.wrappedNode.rhs));
        }
    }

    public static class ComparableAJCTypeCast extends AJCComparableExpressionTree<AJCTypeCast> {
        public ComparableAJCTypeCast(AJCTypeCast wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCTypeCast)) {
                return false;
            }

            ComparableAJCTypeCast cast = (ComparableAJCTypeCast) obj;

            return wrap(wrappedNode.clazz).equals(wrap(cast.wrappedNode.clazz))
                && wrap(wrappedNode.expr).equals(wrap(cast.wrappedNode.expr));
        }
    }

    public static class ComparableAJCInstanceOf extends AJCComparableExpressionTree<AJCInstanceOf> {
        public ComparableAJCInstanceOf(AJCInstanceOf wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCInstanceOf)) {
                return false;
            }

            ComparableAJCInstanceOf cast = (ComparableAJCInstanceOf) obj;
            return wrap(wrappedNode.expr).equals(wrap(cast.wrappedNode.expr))
                && wrappedNode.clazz.getTargetSymbol().equals(cast.wrappedNode.clazz.getTargetSymbol());
        }
    }

    public static class ComparableAJCArrayAccess extends AJCComparableExpressionTree<AJCArrayAccess> {
        public ComparableAJCArrayAccess(AJCArrayAccess wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCArrayAccess)) {
                return false;
            }

            ComparableAJCArrayAccess cast = (ComparableAJCArrayAccess) obj;
            return wrap(wrappedNode.indexed).equals(wrap(cast.wrappedNode.indexed))
                && wrap(wrappedNode.index).equals(wrap(cast.wrappedNode.index));
        }
    }

    public static class ComparableAJCSymbolRefTree<Q extends Symbol> extends AJCComparableExpressionTree<AJCSymbolRefTree<Q>> {
        public ComparableAJCSymbolRefTree(AJCSymbolRefTree<Q> wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCSymbolRefTree)) {
                return false;
            }

            ComparableAJCSymbolRefTree<Q> cast = (ComparableAJCSymbolRefTree<Q>) obj;

            return wrappedNode.getTargetSymbol().equals(cast.wrappedNode.getTargetSymbol());
        }
    }

    public static class ComparableAJCLiteral extends AJCComparableExpressionTree<AJCLiteral> {
        public ComparableAJCLiteral(AJCLiteral wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCLiteral)) {
                return false;
            }

            ComparableAJCLiteral cast = (ComparableAJCLiteral) obj;

            if (wrappedNode.getKind() != cast.wrappedNode.getKind()) {
                return false;
            }

            // Since the value of the null literal is actually null, using equals() ends badly..
            if (wrappedNode.getKind() == Kind.NULL_LITERAL) {
                return true;
            }

            return wrappedNode.getValue().equals(cast.wrappedNode.getValue());
        }
    }

    public static class ComparableAJCPrimitiveTypeTree extends AJCComparableExpressionTree<AJCPrimitiveTypeTree> {
        public ComparableAJCPrimitiveTypeTree(AJCPrimitiveTypeTree wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCPrimitiveTypeTree)) {
                return false;
            }

            ComparableAJCPrimitiveTypeTree cast = (ComparableAJCPrimitiveTypeTree) obj;

            return wrappedNode.getDecoratedTree().typetag == cast.wrappedNode.getDecoratedTree().typetag;
        }
    }

    // Object type trees are handled ad SymbolRefTree<TypeSymbol>.


    public static class ComparableAJCArrayTypeTree extends AJCComparableExpressionTree<AJCArrayTypeTree> {
        public ComparableAJCArrayTypeTree(AJCArrayTypeTree wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCArrayTypeTree)) {
                return false;
            }

            ComparableAJCArrayTypeTree cast = (ComparableAJCArrayTypeTree) obj;

            // Just like a SymbolRefTree<TypeSymbol>, but it can only match another ArrayTypeTree.
            return wrappedNode.getTargetSymbol().equals(cast.wrappedNode.getTargetSymbol());
        }
    }

    public static class ComparableAJCTypeUnion extends AJCComparableExpressionTree<AJCTypeUnion> {
        public ComparableAJCTypeUnion(AJCTypeUnion wrappedNode) {
            super(wrappedNode);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComparableAJCTypeUnion)) {
                return false;
            }

            ComparableAJCTypeUnion cast = (ComparableAJCTypeUnion) obj;

            AJCTypeExpression[] alternatives = wrappedNode.alternatives.toArray(new AJCTypeExpression[5]);
            AJCTypeExpression[] alternativesCast = cast.wrappedNode.alternatives.toArray(new AJCTypeExpression[5]);

            for (int i = 0; i < alternatives.length; i++) {
                if (!wrap(alternatives[i]).equals(wrap(alternativesCast[i]))) {
                    return false;
                }
            }

            return true;
        }
    }
}
