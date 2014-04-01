package joust.optimisers.avail.normalisedexpressions;

import com.sun.source.tree.Scope;
import com.sun.tools.javac.code.Symbol;
import joust.utils.LogUtils;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;

/**
 * An object that may, or may not, be a VarSymbol.
 * If it's not a VarSymbol, it's a placeholder for one. It can be induced to create the VarSymbol.
 * Uses a hashmap to prevent multiple symbols pointing to the same symbol from existing.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class PossibleSymbol {
    static HashMap<VarSymbol, PossibleSymbol> concreteSymbols;
    static int symbolId;

    public static void init() {
        concreteSymbols = new HashMap<>();
        symbolId = 0;
    }

    int instanceId;
    VarSymbol sym;

    public PossibleSymbol() {
        instanceId = symbolId++;
    }

    private PossibleSymbol(VarSymbol s) {
        this();
        sym = s;
    }

    public boolean isConcrete() {
        return sym != null;
    }

    /**
     * Set this symbol to one that represents the given concrete symbol.
     *
     * @param s The VarSymbol this PossibleSymbol should begin to represent.
     */
    public void setSym(VarSymbol s) {
        sym = s;

        if (!concreteSymbols.containsKey(s)) {
            concreteSymbols.put(s, this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PossibleSymbol)) {
            return false;
        }

        PossibleSymbol cast = (PossibleSymbol) obj;
        if (cast.instanceId == instanceId) {
            return true;
        }

        if (sym != null) {
            return sym.equals(cast.sym);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return instanceId;
    }

    public VarSymbol getOrCreateTemporary(Symbol owner) {
        if (sym != null) {
            return sym;
        }

        // temporaryVariable = new VarSymbol(names.fromString())
        return null;
    }

    /**
     * Get or create the PossibleSymbol wrapping the given concrete symbol.
     * @param s The VarSymbol to fetch a concrete PossibleSymbol for.
     * @return The existing PossibleSymbol for s, if any, or a fresh one, if none existed before.
     */
    public static PossibleSymbol getConcrete(VarSymbol s) {
        if (concreteSymbols.containsKey(s)) {
            return concreteSymbols.get(s);
        }

        PossibleSymbol sym = new PossibleSymbol(s);
        concreteSymbols.put(s, sym);
        return sym;
    }

    @Override
    public String toString() {
        return "Sym("+instanceId+":"+sym+")";
    }
}
