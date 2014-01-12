package joust.optimisers.avail;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import lombok.Data;

public @Data class AvailableExpression {
    JCTree.JCExpression expr;
    Symbol availableWhere;
}
