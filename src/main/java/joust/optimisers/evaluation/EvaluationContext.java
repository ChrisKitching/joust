package joust.optimisers.evaluation;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.utils.TreeUtils;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
/**
 * A context for evaluating expressions at compile-time.
 * Or rather, trying to.
 */
public @Log4j2
class EvaluationContext {
    @Getter HashMap<VarSymbol, Value> currentAssignments = new HashMap<>();

    // An immutable map that relates the assignop opcodes to their non-assigning equivalents.
    private final static Map<Tag, Tag> OPASG_TO_OP;
    static {
        final Map<Tag, Tag> map = new HashMap<Tag, Tag>() {
            {
                put(Tag.BITOR_ASG, Tag.BITOR);
                put(Tag.BITXOR_ASG, Tag.BITXOR);
                put(Tag.BITAND_ASG, Tag.BITAND);
                put(Tag.SL_ASG, Tag.SL);
                put(Tag.SR_ASG, Tag.SR);
                put(Tag.USR_ASG, Tag.USR);
                put(Tag.PLUS_ASG, Tag.PLUS);
                put(Tag.MINUS_ASG, Tag.MINUS);
                put(Tag.MUL_ASG, Tag.MUL);
                put(Tag.DIV_ASG, Tag.DIV);
                put(Tag.MOD_ASG, Tag.MOD);
            }
        };

        OPASG_TO_OP = Collections.unmodifiableMap(map);
    }

    /**
     * Evaluate statements. The only sort of statement allowed are variable declarations.
     */
    public void evaluate(JCStatement statement) {
        log.debug("Evaluating: {}", statement);
        if (statement instanceof JCVariableDecl) {
            JCVariableDecl cast = (JCVariableDecl) statement;
            currentAssignments.put(cast.sym, Value.UNKNOWN);
            if (cast.init != null) {
                Value val = evaluate(cast.init);
                currentAssignments.put(cast.sym, val);
            }
        } else if (statement instanceof JCExpressionStatement) {
            evaluate(((JCExpressionStatement) statement).expr);
        }
    }
    public void evaluateStatements(List<JCStatement> statements) {
        for (JCStatement stat : statements) {
            evaluate(stat);
        }
    }
    public void evaluateExpressionStatements(List<JCExpressionStatement> statements) {
        for (JCStatement stat : statements) {
            evaluate(stat);
        }
    }

    /**
     * Evaluate the given expression in this context, update according to any side-effects it has, and return the value
     * of the given expression in this context.
     */
    public Value evaluate(JCAssign assign) {
        VarSymbol sym = TreeUtils.getTargetSymbolForAssignment(assign);
        Value val = evaluate(assign.rhs);
        currentAssignments.put(sym, val);
        return val;
    }

    public Value evaluate(JCAssignOp assignOp) {
        VarSymbol targetSym = TreeUtils.getTargetSymbolForExpression(assignOp);

        Value rhs = evaluate(assignOp.getExpression());
        Value currentValue = currentAssignments.get(targetSym);

        Tag opcode = OPASG_TO_OP.get(assignOp.getTag());
        Value newValue = Value.binary(opcode, currentValue, rhs);
        currentAssignments.put(targetSym, newValue);

        return newValue;
    }

    public Value evaluate(JCBinary binary) {
        Value rVal = evaluate(binary.lhs);
        Value lVal = evaluate(binary.rhs);

        return Value.binary(binary.getTag(), rVal, lVal);
    }

    public Value evaluate(JCUnary unary) {
        log.debug("Evaluating unary: {}", unary);
        Value arg = evaluate(unary.arg);

        Value result = null;
        Tag opcode = unary.getTag();

        int inc;
        switch (opcode) {
            case POSTINC:
                inc = 1;
                result = arg;
                break;
            case POSTDEC:
                inc = -1;
                result = arg;
                break;
            case PREINC:
                inc = 1;
                break;
            case PREDEC:
                inc = -1;
                break;
            default:
                // Whew! No side-effects to think about...
                return Value.unary(unary.getTag(), arg);
        }

        // Having sorted the return value, now apply the side effect...
        VarSymbol target = TreeUtils.getTargetSymbolForExpression(unary.arg);

        // Increment the stored value for this VarSymbol.
        Value existingValue = currentAssignments.get(target);
        Value newValue = Value.binary(Tag.PLUS, existingValue, Value.of(inc));
        if (result == null) {
            result = newValue;
        }

        currentAssignments.put(target, newValue);

        return result;
    }

    public Value evaluate(JCTypeCast cast) {
        // TODO: Get cleverer at this.
        return Value.UNKNOWN;
    }

    public Value evaluate(JCInstanceOf instanceOf) {
        // TODO: Get cleverer at this.
        return Value.UNKNOWN;
    }

    public Value evaluate(JCArrayAccess arrayAccess) {
        // TODO: Get cleverer at this.
        return Value.UNKNOWN;
    }

    public Value evaluate(JCFieldAccess fieldAccess) {
        // TODO: Get cleverer at this.
        return Value.UNKNOWN;
    }

    public Value evaluate(JCLiteral literal) {
        return Value.of(literal.value);
    }

    public Value evaluate(JCIdent ident) {
        Symbol sym = ident.sym;
        if (sym instanceof VarSymbol && currentAssignments.containsKey(sym)) {
            return currentAssignments.get(sym);
        }

        return Value.UNKNOWN;
    }

    public Value evaluate(JCExpression e) {
        // More horrors.
        if (e instanceof JCAssign) {
            return evaluate((JCAssign) e);
        } else if (e instanceof JCAssignOp) {
            return evaluate((JCAssignOp) e);
        } else if (e instanceof JCBinary) {
            return evaluate((JCBinary) e);
        } else if (e instanceof JCUnary) {
            return evaluate((JCUnary) e);
        } else if (e instanceof JCTypeCast) {
            return evaluate((JCTypeCast) e);
        } else if (e instanceof JCInstanceOf) {
            return evaluate((JCInstanceOf) e);
        } else if (e instanceof JCArrayAccess) {
            return evaluate((JCArrayAccess) e);
        } else if (e instanceof JCFieldAccess) {
            return evaluate((JCFieldAccess) e);
        } else if (e instanceof JCLiteral) {
            return evaluate((JCLiteral) e);
        }  else if (e instanceof JCIdent) {
            return evaluate((JCIdent) e);
        } else {
            log.warn("Unknown expression type: {}", e);
            return Value.UNKNOWN;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (VarSymbol sym : currentAssignments.keySet()) {
            sb.append(sym).append(" -> ").append(currentAssignments.get(sym));
        }

        return sb.toString();
    }
}
