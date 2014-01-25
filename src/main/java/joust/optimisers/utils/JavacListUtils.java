package joust.optimisers.utils;

import com.sun.tools.javac.util.List;

/**
 * The list functions Javac didn't want you to have. Provided as static functions instead of extending List
 * so you can still interact with javac classes. You can just now do cleverer things to their lists.
 */
public class JavacListUtils {
    public static<T> List<T> insertAtIndex(final List<T> target, final T element, final int index) {
        List<T> currentTarget = target;
        List<T> lastTarget = null;

        int currentIndex = index;

        // Run down the list until you reach the index you care about...
        while (currentIndex != 0) {
            lastTarget = currentTarget;
            currentTarget = currentTarget.tail;
            currentIndex--;
        }

        // Stick the new element onto the head of the tail of the target list.
        List<T> newNode = currentTarget.prepend(element);
        if (lastTarget != null) {
            lastTarget.setTail(newNode);
        }

        if (index == 0) {
            return newNode;
        }
        return target;
    }
}
