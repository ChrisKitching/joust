package joust.optimisers.illegaloverride;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.ClassSymbol;
import static com.sun.tools.javac.code.Symbol.MethodSymbol;
import static joust.tree.annotatedtree.AJCTree.AJCMethodDecl;
import static joust.utils.compiler.StaticCompilerUtils.types;

/**
 * Detects attempts to illegally override a package-private method. This has unpleasant semantics on
 * versions of Android earlier than 4.1, so should at least be a compiler warning.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class IllegalOverrideVisitor extends BaseTranslator {
    @Override
    protected void visitMethodDef(AJCMethodDecl that) {
        super.visitMethodDef(that);

        // Abort if the method is not package-private.
        AJCTree.AJCModifiers modifiers = that.mods;
        if ((modifiers.getDecoratedTree().flags & Flags.AccessFlags) != 0) {
            return;
        }

        final MethodSymbol targetSymbol = that.getTargetSymbol();

        // Constructors don't count...
        if (targetSymbol.isConstructor()) {
            return;
        }

        // Nor do <clinit> or instance initialisers...
        if (targetSymbol.isStaticOrInstanceInit()) {
            return;
        }

        // Match by signature, since these methods don't *really* override each other. It's just a
        // Dalvik bug that they do on stupid versions of Dalvik.
        String ourHash = getMethodSignatureHash(targetSymbol);

        ClassSymbol owner = (ClassSymbol) targetSymbol.owner;

        Type superType = types.supertype(owner.asType());
        log.info("Owning class: " + owner.fullname);

        while (superType.tsym != null) {
            log.info("superType " + superType);
            log.info("superType.tSym " + superType.tsym);
            final ClassSymbol csym = (ClassSymbol) superType.tsym;

            // Check every method in this superType to see if we're a signature match for it...
            final Scope classMembers = csym.members();
            for (Symbol symbol : classMembers.getElements()) {
                if (!(symbol instanceof MethodSymbol)) {
                    continue;
                }

                String theirHash = getMethodSignatureHash((MethodSymbol) symbol);
                if (theirHash.equals(ourHash)) {
                    log.warn("{}.{} is implicitly overriding {}.{}", targetSymbol.owner, targetSymbol, symbol.owner, symbol);
                }
            }

            // Move another level up the class tree...
            superType = types.supertype(superType);
        }
    }

    /**
     * Get a string uniquely representing the name and argument types of this method.
     * These can be compared to determine if a method is effectively-overiding another.
     */
    private String getMethodSignatureHash(MethodSymbol sym) {
        StringBuilder hash = new StringBuilder(sym.name.toString());
        for (Symbol.VarSymbol symbol : sym.getParameters()) {
            hash.append(symbol.type.toString());
        }

        return hash.toString();
    }
}
