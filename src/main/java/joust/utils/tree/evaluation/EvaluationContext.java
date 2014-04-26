package joust.utils.tree.evaluation;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.utils.logging.LogUtils;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.Tag;
import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
/**
 * A context for evaluating expressions at compile-time.
 * Or rather, trying to.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public
class EvaluationContext {
    @Getter HashMap<VarSymbol, Value> currentAssignments = new HashMap<VarSymbol, Value>();

    // An immutable map that relates the assignop opcodes to their non-assigning equivalents.
    private static final Map<Tag, Tag> OPASG_TO_OP;
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
    public void evaluate(AJCStatement statement) {
        log.debug("Evaluating: {}", statement);
        AJCVariableDecl cast = (AJCVariableDecl) statement;
        currentAssignments.put(cast.getTargetSymbol(), Value.UNKNOWN);
        if (cast.getInit() != null) {
            Value val = evaluate(cast.getInit());
            currentAssignments.put(cast.getTargetSymbol(), val);
        }
    }
    public void evaluate(AJCExpressionStatement exec) {
        evaluate(exec.expr);
    }

    public void evaluateStatements(List<AJCStatement> statements) {
        for (AJCStatement stat : statements) {
            evaluate(stat);
        }
    }
    public void evaluateExpressionStatements(List<AJCExpressionStatement> statements) {
        for (AJCExpressionStatement stat : statements) {
            evaluate(stat);
        }
    }

    /**
     * Evaluate the given expression in this context, update according to any side-effects it has, and return the value
     * of the given expression in this context.
     */
    public Value evaluate(AJCAssign assign) {
        VarSymbol sym = assign.getTargetSymbol();
        Value val = evaluate(assign.rhs);
        currentAssignments.put(sym, val);
        return val;
    }

    public Value evaluate(AJCAssignOp assignOp) {
        VarSymbol targetSym = assignOp.getTargetSymbol();

        Value rhs = evaluate(assignOp.rhs);
        Value currentValue = currentAssignments.get(targetSym);

        Tag opcode = OPASG_TO_OP.get(assignOp.getTag());
        Value newValue = Value.binary(opcode, currentValue, rhs);
        currentAssignments.put(targetSym, newValue);

        return newValue;
    }

    public Value evaluate(AJCBinary binary) {
        Value rVal = evaluate(binary.lhs);
        Value lVal = evaluate(binary.rhs);

        return Value.binary(binary.getTag(), rVal, lVal);
    }

    public Value evaluate(AJCUnaryAsg unary) {
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
                //Panic
                log.fatal("Unexpected opcode in AJCUnaryAsg evaluation: "+opcode);
                return null;
        }

        // Having sorted the return value, now apply the side effect...
        VarSymbol target = unary.arg.getTargetSymbol();

        // Increment the stored value for this VarSymbol.
        Value existingValue = currentAssignments.get(target);
        if (existingValue == null) {
            return Value.UNKNOWN;
        }

        Value newValue = Value.binary(Tag.PLUS, existingValue, Value.of(inc));
        if (result == null) {
            result = newValue;
        }

        currentAssignments.put(target, newValue);

        return result;
    }

    public Value evaluate(AJCUnary unary) {
        log.debug("Evaluating unary: {}", unary);
        return Value.unary(unary.getTag(), evaluate(unary.arg));
    }

    public Value evaluate(AJCTypeCast cast) {
        // TODO: Get cleverer at this.
        return Value.UNKNOWN;
    }

    public Value evaluate(AJCInstanceOf instanceOf) {
        // TODO: Get cleverer at this.
        return Value.UNKNOWN;
    }

    public Value evaluate(AJCArrayAccess arrayAccess) {
        // TODO: Get cleverer at this.
        return Value.UNKNOWN;
    }

    public Value evaluate(AJCFieldAccess fieldAccess) {
        // TODO: Get cleverer at this.
        return Value.UNKNOWN;
    }

    public Value evaluate(AJCLiteral literal) {
        return Value.of(literal.getValue());
    }

    public Value evaluate(AJCIdent ident) {
        Symbol sym = ident.getTargetSymbol();
        if (sym instanceof VarSymbol && currentAssignments.containsKey(sym)) {
            return currentAssignments.get(sym);
        }

        return Value.UNKNOWN;
    }

    public Value evaluate(AJCExpressionTree e) {
        // More horrors.
        if (e instanceof AJCAssign) {
            return evaluate((AJCAssign) e);
        } else if (e instanceof AJCAssignOp) {
            return evaluate((AJCAssignOp) e);
        } else if (e instanceof AJCBinary) {
            return evaluate((AJCBinary) e);
        } else if (e instanceof AJCUnary) {
            return evaluate((AJCUnary) e);
        } else if (e instanceof AJCUnaryAsg) {
            return evaluate((AJCUnaryAsg) e);
        } else if (e instanceof AJCTypeCast) {
            return evaluate((AJCTypeCast) e);
        } else if (e instanceof AJCInstanceOf) {
            return evaluate((AJCInstanceOf) e);
        } else if (e instanceof AJCArrayAccess) {
            return evaluate((AJCArrayAccess) e);
        } else if (e instanceof AJCFieldAccess) {
            return evaluate((AJCFieldAccess) e);
        } else if (e instanceof AJCLiteral) {
            return evaluate((AJCLiteral) e);
        }  else if (e instanceof AJCIdent) {
            return evaluate((AJCIdent) e);
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
