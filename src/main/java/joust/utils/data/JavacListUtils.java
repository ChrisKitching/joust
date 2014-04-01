package joust.utils.data;

import com.sun.tools.javac.util.List;
import joust.utils.logging.LogUtils;
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * Javac's List needs... Alteration... For my purposes.
 * Since their constructor is package-protected - I had to improvise. This class is spliced onto Javac's list class
 * using Lombok's ExtensionMethod facility.
 *
 * This flavour of the list *is* mutable. It implements some of the unsupported operations of Javac's list, and
 * is particularly useful for rewriting trees.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class JavacListUtils {
    // Error strings. Handy for JUnit...
    public static final String INSERT_NEGATIVE = "Attempt to add element at negative index in List!";
    public static final String INSERT_OUT_OF_BOUNDS = "Attempt to add beyond end of list!";
    public static final String SET_OUT_OF_BOUNDS = "Attempt to set value beyond end of List!";
    public static final String REMOVE_NEGATIVE = "Attempt to remove negatively-indexed value from List!";
    public static final String REMOVE_OUT_OF_BOUNDS = "Attempt to remove value beyond end of List!";
    public static final String REMOVE_NONEXISTENT = "Attempt to remove nonexistent value from List!";
    public static final String REPLACE_NONEXISTENT = "Attempt to replace a nonexistent value in a List!";

    /**
     * Get the last List node in the given list (As opposed to javac's List.last() which returns the last *element*).
     */
    public static<T> List<T> lastNonNilNode(@NonNull List<T> list) {
        while (list.tail != null && list.tail != List.nil()) {
            list = list.tail;
        }

        return list;
    }

    /**
     * Insert the list e of elements into the list list at index i, returning the affected list, or the new list if the
     * reference node has changed.
     */
    public static<T> List<T> addAtIndex(@NonNull List<T> list, int i, @NonNull List<T> e) {
        if (i < 0) {
            throw new IllegalArgumentException(INSERT_NEGATIVE);
        }

        if (i == 0) {
            // Point the end of the given list to this list.
            List<T> lastNode = lastNonNilNode(e);
            lastNode.tail = list;
            return e;
        }

        List<T> currentTarget = list;
        List<T> previousTarget = null;

        // Run down the list until you find the index of interest.
        while (i > 1) {
            i--;
            previousTarget = currentTarget;
            currentTarget = currentTarget.tail;

            if (currentTarget == List.nil()) {
                throw new IndexOutOfBoundsException(INSERT_OUT_OF_BOUNDS);
            }
        }

        // If you're inserting at the end, drop the nil and point to the new list.
        if (currentTarget == List.nil()) {
            if (previousTarget != null) {
                previousTarget.tail = e;
            } else {
                log.error("Encountered impossible situation in addAtIndex!");
                return e;
            }
        } else {
            List<T> oldTail = currentTarget.tail;
            currentTarget.tail = e;
            lastNonNilNode(e).tail = oldTail;
        }

        return list;
    }
    public static<T> List<T> addAtIndex(@NonNull List<T> list, int i, T e) {
        return addAtIndex(list, i, List.of(e));
    }

    /**
     * Set the element in list list at index i to e.
     */
    public static<T> List<T> set(@NonNull List<T> list, int i, T e) {
        if (i == 0) {
            list.head = e;
            return list;
        }

        List<T> currentTarget = list;
        while (i > 0) {
            i--;
            currentTarget = currentTarget.tail;

            if (currentTarget == null || currentTarget == List.nil()) {
                throw new IndexOutOfBoundsException(SET_OUT_OF_BOUNDS);
            }
        }

        currentTarget.head = e;
        return list;
    }

    /**
     * Remove the element at index i, returning the new list (This list if i > 0).
     */
    public static<T> List<T> removeAtIndex(@NonNull List<T> list, int i) {
        if (i < 0) {
            throw new IllegalArgumentException(REMOVE_NEGATIVE);
        }

        if (list.isEmpty()) {
            throw new IndexOutOfBoundsException(REMOVE_OUT_OF_BOUNDS);
        }

        if (i == 0) {
            return list.tail;
        }

        List<T> beforeVictim = list;

        while (i > 1) {
            i--;
            beforeVictim = beforeVictim.tail;

            if (beforeVictim == List.nil()) {
                throw new IndexOutOfBoundsException(REMOVE_OUT_OF_BOUNDS);
            }
        }

        // Now we want to remove the element ahead of us.

        // Did we hit the end of the list?
        if (beforeVictim == List.nil()) {
            throw new IndexOutOfBoundsException(REMOVE_OUT_OF_BOUNDS);
        }

        // The element to remove.
        List<T> victim = beforeVictim.tail;
        if (victim == List.nil()) {
            throw new IndexOutOfBoundsException(REMOVE_OUT_OF_BOUNDS);
        }

        // The element beyond the target - where currentTarget's tail should point when we're finished.
        List<T> elementBeyondTarget = victim.tail;

        beforeVictim.tail = elementBeyondTarget;

        return list;
    }

    /**
     * Remove one instance of a particular element from the list, if any such element exists. Otherwise, print a warning.
     */
    public static<T> List<T> removeElement(@NonNull List<T> list, T e) {
        int index = list.indexOf(e);
        if (index == -1) {
            log.warn("\nRemoving {} from {} unsuccessful.", e, Arrays.toString(list.toArray()));
            throw new NoSuchElementException(REMOVE_NONEXISTENT);
        }

        return removeAtIndex(list, index);
    }

    /**
     * Replace one instance of target in the list with replacement, returning the new list.
     */
    public static<T> List<T> replace(@NonNull List<T> list, T target, T replacement) {
        // Handle the special case of it being at the front...
        int index = list.indexOf(target);
        if (index == -1) {
            log.warn("Replacing {} with {} in {} unsuccessful.", target, replacement, Arrays.toString(list.toArray()));
            throw new NoSuchElementException(REPLACE_NONEXISTENT);
        }

        return set(list, index, replacement);
    }

    /**
     * Find the index of element target in list `list` using pointer comparism instead of the equals method.
     * @param list
     * @param target
     * @return The index of the found element, of -1 if it is not present.
     */
    public static<T> int dumbIndexOf(@NonNull List<T> list, T target) {
        int i = 0;
        while (list != null) {
            if (list.head == target) {
                return i;
            }

            i++;
            list = list.tail;
        }

        return -1;
    }
}
