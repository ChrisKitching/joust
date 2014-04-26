package joust.optimisers.normalise;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.tree.JCTree.Tag;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * Provide an ordering over expression nodes.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class CommutativitySorterComparator implements Comparator<AJCExpressionTree> {
    // Define a sort-of-mostly-arbitrary ordering on types of expressions.
    // Some vague attempt is made to make expensive operations tend to be on the right of an expression so
    // they're more likely to be short circuited away at runtime. Maybe. We live in hope.
    private static final Map<Class<? extends AJCExpressionTree>, Integer> nodeTypeOrderings;
    static {
        // Irritating immutable map boilerplate...
        final HashMap<Class<? extends AJCExpressionTree>, Integer> orderings = new HashMap<Class<? extends AJCExpressionTree>, Integer>() {
            {
                put(AJCLiteral.class, 1);
                put(AJCIdent.class, 2);
                put(AJCFieldAccess.class, 3);
                put(AJCInstanceOf.class, 4);
                put(AJCUnary.class, 5);
                put(AJCUnaryAsg.class, 6);
                put(AJCBinary.class, 7);
                put(AJCAssign.class, 8);
                put(AJCAssignOp.class, 9);
                put(AJCConditional.class, 10);
                put(AJCCall.class, 11);
                put(AJCTypeCast.class, 12);
                put(AJCEmptyExpression.class, 13);
                put(AJCArrayAccess.class, 14);
            }
        };

        nodeTypeOrderings = Collections.unmodifiableMap(orderings);
    }

    // Impose an arbitrary order over operator tags...
    private static final Map<Tag, Integer> tagOrderings;
    static {
        final Map<Tag, Integer> orderings = new EnumMap<Tag, Integer>(Tag.class) {
            {
                // Binary tags...
                put(Tag.AND, 1);
                put(Tag.BITOR, 2);
                put(Tag.BITXOR, 3);
                put(Tag.BITAND, 4);
                put(Tag.SL, 5);
                put(Tag.SR, 6);
                put(Tag.USR, 7);
                put(Tag.OR, 8);
                put(Tag.EQ, 9);
                put(Tag.NE, 10);
                put(Tag.LT, 11);
                put(Tag.GT, 12);
                put(Tag.LE, 13);
                put(Tag.GE, 14);
                put(Tag.PLUS, 15);
                put(Tag.MINUS, 16);
                put(Tag.MUL, 17);
                put(Tag.DIV, 18);
                put(Tag.MOD, 19);

                // AssignOp tags...
                put(Tag.BITOR_ASG, 20);
                put(Tag.BITXOR_ASG, 21);
                put(Tag.BITAND_ASG, 22);
                put(Tag.SL_ASG, 23);
                put(Tag.SR_ASG, 24);
                put(Tag.USR_ASG, 25);
                put(Tag.PLUS_ASG, 26);
                put(Tag.MINUS_ASG, 27);
                put(Tag.MUL_ASG, 28);
                put(Tag.DIV_ASG, 29);
                put(Tag.MOD_ASG, 30);

                // Unary tags...
                put(Tag.POS, 31);
                put(Tag.NEG, 32);
                put(Tag.NOT, 33);
                put(Tag.COMPL, 34);
                put(Tag.PREINC, 35);
                put(Tag.POSTINC, 36);
                put(Tag.PREDEC, 37);
                put(Tag.POSTDEC, 38);
            }
        };

        tagOrderings = Collections.unmodifiableMap(orderings);
    }

    private static final Map<Kind, Integer> literalKindOrderings;
    static {
        final Map<Kind, Integer> orderings = new EnumMap<Kind, Integer>(Kind.class) {
            {
                put(Kind.CHAR_LITERAL, 1);
                put(Kind.INT_LITERAL, 2);
                put(Kind.LONG_LITERAL, 3);
                put(Kind.DOUBLE_LITERAL, 4);
                put(Kind.FLOAT_LITERAL, 5);
                put(Kind.STRING_LITERAL, 6);
                put(Kind.BOOLEAN_LITERAL, 7);
                put(Kind.NULL_LITERAL, 8);
            }
        };

        literalKindOrderings = Collections.unmodifiableMap(orderings);
    }


    @Override
    public int compare(AJCExpressionTree l, AJCExpressionTree r) {
        // Firstly, we sort by type.
        int lType = nodeTypeOrderings.get(l.getClass());
        int rType = nodeTypeOrderings.get(r.getClass());
        if (lType > rType) {
            return 1;
        } else if (rType > lType) {
            return -1;
        }

        // If they're the same, hand over to the various specialised functions for choosing between them...
        if (l instanceof AJCLiteral) {
            return compareLiterals((AJCLiteral) l, (AJCLiteral) r);
        } else if (l instanceof AJCIdent) {
            return compareIdents((AJCIdent) l, (AJCIdent) r);
        } else if (l instanceof AJCFieldAccess) {
            return compareFieldAccesses((AJCFieldAccess) l, (AJCFieldAccess) r);
        } else if (l instanceof AJCInstanceOf) {
            return compareInstanceOfs((AJCInstanceOf) l, (AJCInstanceOf) r);
        } else if (l instanceof AJCUnary) {
            return compareUnaries((AJCUnary) l, (AJCUnary) r);
        } else if (l instanceof AJCUnaryAsg) {
            return compareUnaryAsg((AJCUnaryAsg) l, (AJCUnaryAsg) r);
        } else if (l instanceof AJCBinary) {
            return compareBinaries((AJCBinary) l, (AJCBinary) r);
        } else if (l instanceof AJCAssign) {
            return compareAssigns((AJCAssign) l, (AJCAssign) r);
        } else if (l instanceof AJCAssignOp) {
            return compareAssignOps((AJCAssignOp) l, (AJCAssignOp) r);
        } else if (l instanceof AJCConditional) {
            return compareConditionals((AJCConditional) l, (AJCConditional) r);
        } else if (l instanceof AJCCall) {
            return compareCalls((AJCCall) l, (AJCCall) r);
        } else if (l instanceof AJCArrayAccess) {
            return compareArrayAccesses((AJCArrayAccess) l, (AJCArrayAccess) r);
        } else if (l instanceof AJCTypeCast) {
            return compareTypeCasts((AJCTypeCast) l, (AJCTypeCast) r);
        } else if (l instanceof AJCEmptyExpression) {
            log.warn("Comparing two empty expressions for {}:{}", l, r);
            return 0;
        } else {
            log.fatal("Unexpected expression type: " + l.getClass().getCanonicalName());
            return 0;
        }
    }

    private int compareTypeCasts(AJCTypeCast l, AJCTypeCast r) {
        Type tl = l.clazz.getNodeType();
        Type tr = r.clazz.getNodeType();

        int typeComparism = tl.toString().compareTo(tr.toString());
        if (typeComparism != 0) {
            return typeComparism;
        }

        return compare(l.expr, r.expr);
    }

    private int compareArrayAccesses(AJCArrayAccess l, AJCArrayAccess r) {
        VarSymbol a1 = l.getTargetSymbol();
        VarSymbol a2 = r.getTargetSymbol();

        // Due to unconventional array access limitations....
        if (a1 == null && a2 != null) {
            return 1;
        }

        if (a2 == null && a1 != null) {
            return -1;
        }

        if (a1 != null) {
            int symComp = a1.name.toString().compareTo(a2.name.toString());
            if (symComp != 0) {
                return symComp;
            }
        }

        int indComp = compare(l.index, r.index);
        if (indComp != 0) {
            return indComp;
        }

        return 0;
    }

    private int compareCalls(AJCCall l, AJCCall r) {
        MethodSymbol mSymL = l.getTargetSymbol();
        MethodSymbol mSymR = r.getTargetSymbol();

        int lParams = mSymL.params().length();
        int rParams = mSymR.params().length();
        if (lParams > rParams) {
            return 1;
        } else if (rParams > lParams) {
            return -1;
        }

        return mSymL.name.toString().compareTo(mSymR.name.toString());
    }

    private int compareConditionals(AJCConditional l, AJCConditional r) {
        int condCheck = compare(l.cond, r.cond);
        if (condCheck != 0) {
            return condCheck;
        }

        int thenCheck = compare(l.truepart, r.truepart);
        if (thenCheck != 0) {
            return thenCheck;
        }

        int elseCheck = compare(l.falsepart, r.falsepart);
        if (elseCheck != 0) {
            return elseCheck;
        }

        return 0;
    }

    private int compareAssignOps(AJCAssignOp l, AJCAssignOp r) {
        int lTag = tagOrderings.get(l.getTag());
        int rTag = tagOrderings.get(r.getTag());

        if (lTag > rTag) {
            return 1;
        } else if (rTag > lTag) {
            return -1;
        }

        VarSymbol lSym = l.getTargetSymbol();
        VarSymbol rSym = r.getTargetSymbol();

        return lSym.name.toString().compareTo(rSym.name.toString());
    }

    private int compareAssigns(AJCAssign l, AJCAssign r) {
        VarSymbol lSym = l.getTargetSymbol();
        VarSymbol rSym = r.getTargetSymbol();

        return lSym.name.toString().compareTo(rSym.name.toString());
    }

    private int compareUnaries(AJCUnary l, AJCUnary r) {
        int lTag = tagOrderings.get(l.getTag());
        int rTag = tagOrderings.get(r.getTag());

        if (lTag > rTag) {
            return 1;
        } else if (rTag > lTag) {
            return -1;
        }

        return compare(l.arg, r.arg);
    }

    private int compareUnaryAsg(AJCUnaryAsg l, AJCUnaryAsg r) {
        int lTag = tagOrderings.get(l.getTag());
        int rTag = tagOrderings.get(r.getTag());

        if (lTag > rTag) {
            return 1;
        } else if (rTag > lTag) {
            return -1;
        }

        return compare(l.arg, r.arg);
    }

    private int compareInstanceOfs(AJCInstanceOf l, AJCInstanceOf r) {
        TypeSymbol lClazz = l.clazz.getTargetSymbol();
        TypeSymbol rClazz = r.clazz.getTargetSymbol();

        int clazzCheck = lClazz.name.toString().compareTo(rClazz.name.toString());
        if (clazzCheck != 0) {
            return clazzCheck;
        }

        return compare(l.expr, r.expr);
    }

    private int compareFieldAccesses(AJCFieldAccess l, AJCFieldAccess r) {
        Symbol lSym = l.getTargetSymbol();
        Symbol rSym = l.getTargetSymbol();

        return lSym.name.toString().compareTo(rSym.name.toString());
    }

    private int compareBinaries(AJCBinary l, AJCBinary r) {
        int lTag = tagOrderings.get(l.getTag());
        int rTag = tagOrderings.get(r.getTag());

        if (lTag > rTag) {
            return 1;
        } else if (rTag > lTag) {
            return -1;
        }

        int leftCheck = compare(l.lhs, r.lhs);
        if (leftCheck != 0) {
            return leftCheck;
        }

        int rightCheck = compare(l.rhs, r.rhs);
        if (rightCheck != 0) {
            return rightCheck;
        }

        leftCheck = compare(l.lhs, l.rhs);
        if (leftCheck != 0) {
            return leftCheck;
        }

        return compare(r.rhs, r.rhs);
    }

    private int compareIdents(AJCIdent l, AJCIdent r) {
        return l.getName().toString().compareTo(r.getName().toString());
    }

    private int compareLiterals(AJCLiteral l, AJCLiteral r) {
        int lKind = literalKindOrderings.get(l.getKind());
        int rKind = literalKindOrderings.get(r.getKind());

        if (lKind > rKind) {
            return 1;
        } else if (rKind > lKind) {
            return -1;
        }

        // Dirty hackage!
        // TODO: Small performance gain perhaps possible from doing this more sensibly...?
        return l.toString().compareTo(r.toString());
    }
}
