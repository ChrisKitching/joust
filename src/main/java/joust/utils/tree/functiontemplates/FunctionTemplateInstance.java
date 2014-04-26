package joust.utils.tree.functiontemplates;

import com.sun.tools.javac.util.List;
import lombok.AllArgsConstructor;

import static joust.tree.annotatedtree.AJCTree.*;

@AllArgsConstructor
public class FunctionTemplateInstance {
    // The substituted body of the function.
    public final AJCExpressionTree body;

    // The list of statements that must be run before the call to initialise the arguments.
    // Typically, variable declarations for copying arguments to temporaries.
    public final List<AJCStatement> startup;
}
