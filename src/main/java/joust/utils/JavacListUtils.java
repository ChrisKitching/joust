package joust.utils;

import com.sun.tools.javac.util.List;
import lombok.extern.log4j.Log4j2;

/**
 * Javac's List needs... Alteration... For my purposes.
 * Since their constructor is package-protected - I had to improvise.
 *
 * This flavour of the list *is* mutable. It implements some of the unsupported operations of Javac's list, and
 * is particularly useful for rewriting trees.
 */
public @Log4j2 class JavacListUtils {
    /**
     * Add a new element to the given list at the given index.
     * @return The input list or, if the element was added at the front of the list, the new node that results.
     */
    public static<T> List<T> addAtIndex(List<T> list, int i, T e) {
        if (i < 0) {
            throw new IllegalArgumentException("Attempt to add element at negative index in List!");
        }

        if (i == 0) {
            List<T> ret = List.of(e);
            ret.tail = list;
            return ret;
        }

        List<T> currentTarget = list;

        // Run down the list until you find the index of interest.
        while (i > 0) {
            if (currentTarget == List.nil()) {
                throw new IndexOutOfBoundsException("Attempt to add value to unreachable index in List.");
            }
            i--;
            currentTarget = currentTarget.tail;
        }

        // Add the new element.
        List<T> newTail = List.of(e);
        newTail.tail = currentTarget.tail;
        currentTarget.tail = newTail;

        return list;
    }

    public static<T> List<T> set(List<T> list, int i, T e) {
        if (i == 0) {
            List<T> ret = List.of(e);
            ret.tail = list;
            return ret;
        }

        List<T> currentTarget = list;
        while (i > 0) {
            i--;
            currentTarget = currentTarget.tail;

            if (currentTarget == List.nil()) {
                throw new IndexOutOfBoundsException("Attempt to set value at an unreachable index in List.");
            }
        }

        currentTarget.head = e;
        return list;
    }

    /**
     * Remove the element at index i, returning the new list (This list if i > 0).
     */
    public static<T> List<T> removeAtIndex(List<T> list, int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Attempt to remove negatively-indexed value from List.");
        }

        if (i == 0) {
            return list.tail;
        }

        List<T> currentTarget = list;

        while (i > 1) {
            if (currentTarget == List.nil()) {
                throw new IndexOutOfBoundsException("Attempt to remove value greater than index of final value in List from List.");
            }
            i--;
            currentTarget = currentTarget.tail;
        }

        // currentTarget now the element before the element to be removed.
        if (currentTarget == List.nil() || currentTarget.tail == List.nil()) {
            throw new IndexOutOfBoundsException("Attempt to remove value greater than index of final value in List from List.");
        }

        // Cut the unwanted element out of the list.
        currentTarget.tail = currentTarget.tail.tail;

        return list;
    }

    /**
     * Remove one instance of a particular element from the list, if any such element exists. Otherwise, print a warning.
     */
    public static<T> List<T> removeElement(List<T> list, T e) {
        int index = list.indexOf(e);
        if (index == -1) {
            JavacListUtils.log.warn("Attempt to remove nonexistent list element: {} from {}", e, list);
            return list;
        }

        return removeAtIndex(list, index);
    }

    /**
     * Replace one instance of target in the list with replacement, returning the new list.
     * TODO: Silently fails if an instance not found... Problematic?
     */
    public static<T> List<T> replace(List<T> list, T target, T replacement) {
        // Handle the special case of it being at the front...
        List<T> currentList = list;
        while (list != null && currentList.head != target) {
            currentList = list.tail;
        }

        if (currentList != null) {
            currentList.head = replacement;
        }

        return list;
    }
}
