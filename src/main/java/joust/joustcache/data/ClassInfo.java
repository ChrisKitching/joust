package joust.joustcache.data;

import com.sun.tools.javac.code.Symbol;
import lombok.Data;

import java.util.LinkedList;

/**
 * The data we want to assoiciate with each class in the persistent data storage.
 */
public @Data
class ClassInfo {
    public final LinkedList<MethodInfo> methodInfos = new LinkedList<>();
    public int hash;

    public static String getHashForVariable(Symbol.VarSymbol sym) {
        return sym.owner.type.toString() + '.' + sym.name.toString() + ':' + sym.type.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ClassInfo ").append(hash);
        for (MethodInfo mi : methodInfos) {
            sb.append("\n").append(mi.toString());
        }

        return sb.toString();
    }
}
