package joust.optimisers.utils;

import joust.utils.LogUtils;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * Provide an ordering over expression nodes.
 */
public @Log4j2
class CommutativitySorterComparator implements Comparator<JCExpression> {
    // Define a sort-of-mostly-arbitrary ordering on types of expressions.
    // Some vague attempt is made to make expensive operations tend to be on the right of an expression so
    // they're more likely to be short circuited away at runtime. Maybe. We live in hope.
    private static final Map<Class<? extends JCExpression>, Integer> nodeTypeOrderings;
    static {
        // Irritating immutable map boilerplate...
        final HashMap<Class<? extends JCExpression>, Integer> orderings = new HashMap<Class<? extends JCExpression>, Integer>() {
            {
                put(JCLiteral.class, 1);
                put(JCIdent.class, 2);
                put(JCFieldAccess.class, 3);
                put(JCInstanceOf.class, 4);
                put(JCUnary.class, 5);
                put(JCBinary.class, 6);
                put(JCAssign.class, 7);
                put(JCAssignOp.class, 8);
                put(JCConditional.class, 9);
                put(JCMethodInvocation.class, 10);
            }
        };

        nodeTypeOrderings = Collections.unmodifiableMap(orderings);
    }

    // Impose an arbitrary order over operator tags...
    private static final Map<Tag, Integer> tagOrderings;
    static {
        final HashMap<Tag, Integer> orderings = new HashMap<Tag, Integer>() {
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
        final HashMap<Kind, Integer> orderings = new HashMap<Kind, Integer>() {
            {
                put(Kind.CHAR_LITERAL, 1);
                put(Kind.INT_LITERAL, 2);
                put(Kind.LONG_LITERAL, 3);
                put(Kind.DOUBLE_LITERAL, 4);
                put(Kind.FLOAT_LITERAL, 5);
                put(Kind.STRING_LITERAL, 6);
            }
        };

        literalKindOrderings = Collections.unmodifiableMap(orderings);
    }


    @Override
    public int compare(JCExpression l, JCExpression r) {
        // Firstly, we sort by type.
        int lType = nodeTypeOrderings.get(l.getClass());
        int rType = nodeTypeOrderings.get(r.getClass());
        if (lType > rType) {
            return 1;
        } else if (rType > lType) {
            return -1;
        }

        // If they're the same, hand over to the various specialised functions for choosing between them...
        if (l instanceof JCLiteral) {
            return compareLiterals((JCLiteral) l, (JCLiteral) r);
        } else if (l instanceof JCIdent) {
            return compareIdents((JCIdent) l, (JCIdent) r);
        } else if (l instanceof JCFieldAccess) {
            return compareFieldAccesses((JCFieldAccess) l, (JCFieldAccess) r);
        } else if (l instanceof JCInstanceOf) {
            return compareInstanceOfs((JCInstanceOf) l, (JCInstanceOf) r);
        } else if (l instanceof JCUnary) {
            return compareUnaries((JCUnary) l, (JCUnary) r);
        } else if (l instanceof JCBinary) {
            return compareBinaries((JCBinary) l, (JCBinary) r);
        } else if (l instanceof JCAssign) {
            return compareAssigns((JCAssign) l, (JCAssign) r);
        } else if (l instanceof JCAssignOp) {
            return compareAssignOps((JCAssignOp) l, (JCAssignOp) r);
        } else if (l instanceof JCConditional) {
            return compareConditionals((JCConditional) l, (JCConditional) r);
        } else if (l instanceof JCMethodInvocation) {
            return compareCalls((JCMethodInvocation) l, (JCMethodInvocation) r);
        } else {
            LogUtils.raiseCompilerError("Unexpected expression type: " + l.getClass().getCanonicalName());
            return 0;
        }
    }

    private int compareCalls(JCMethodInvocation l, JCMethodInvocation r) {
        MethodSymbol mSymL = TreeUtils.getTargetSymbolForCall(l);
        MethodSymbol mSymR = TreeUtils.getTargetSymbolForCall(r);

        int lParams = mSymL.params().length();
        int rParams = mSymR.params().length();
        if (lParams > rParams) {
            return 1;
        } else if (rParams > lParams) {
            return -1;
        }

        return mSymL.name.toString().compareTo(mSymR.name.toString());
    }

    private int compareConditionals(JCConditional l, JCConditional r) {
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

    private int compareAssignOps(JCAssignOp l, JCAssignOp r) {
        int lTag = tagOrderings.get(l.getTag());
        int rTag = tagOrderings.get(r.getTag());

        if (lTag > rTag) {
            return 1;
        } else if (rTag > lTag) {
            return -1;
        }

        VarSymbol lSym = TreeUtils.getTargetSymbolForAssignment(l);
        VarSymbol rSym = TreeUtils.getTargetSymbolForAssignment(r);

        return lSym.name.toString().compareTo(rSym.name.toString());
    }

    private int compareAssigns(JCAssign l, JCAssign r) {
        VarSymbol lSym = TreeUtils.getTargetSymbolForAssignment(l);
        VarSymbol rSym = TreeUtils.getTargetSymbolForAssignment(r);

        return lSym.name.toString().compareTo(rSym.name.toString());
    }

    private int compareUnaries(JCUnary l, JCUnary r) {
        int lTag = tagOrderings.get(l.getTag());
        int rTag = tagOrderings.get(r.getTag());

        if (lTag > rTag) {
            return 1;
        } else if (rTag > lTag) {
            return -1;
        }

        return compare(l.arg, r.arg);
    }

    private int compareInstanceOfs(JCInstanceOf l, JCInstanceOf r) {
        VarSymbol lSym = TreeUtils.getTargetSymbolForExpression(l);
        VarSymbol rSym = TreeUtils.getTargetSymbolForExpression(r);

        int check = lSym.name.toString().compareTo(rSym.name.toString());
        if (check != 0) {
            return check;
        }

        // TODO: Compare class names... Something to get the ClassSymbol from whatever node type that is?
        log.warn("Failing to compare {} and {}\nlClass: {} of type: {}\nrClass: {} of type: {}", l, r, l.clazz, l.clazz.getClass().getSimpleName(), r.clazz, r.clazz.getClass().getSimpleName());
        return 0;
    }

    private int compareFieldAccesses(JCFieldAccess l, JCFieldAccess r) {
        VarSymbol lSym = TreeUtils.getTargetSymbolForExpression(l);
        VarSymbol rSym = TreeUtils.getTargetSymbolForExpression(r);

        return lSym.name.toString().compareTo(rSym.name.toString());
    }

    private int compareBinaries(JCBinary l, JCBinary r) {
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

    private int compareIdents(JCIdent l, JCIdent r) {
        return l.name.toString().compareTo(r.name.toString());
    }

    private int compareLiterals(JCLiteral l, JCLiteral r) {
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
