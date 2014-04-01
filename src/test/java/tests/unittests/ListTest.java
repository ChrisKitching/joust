package tests.unittests;

import com.sun.tools.javac.util.List;
import joust.utils.data.JavacListUtils;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Unit tests for the JavacListUtils class.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class ListTest {
    @Rule public TestName name = new TestName();

    @Test
    public void testInsertionAtStart() {
        List<Integer> someIntegers = List.of(1, 2, 3);
        log.debug("Inserting 4 at the start of {}", Arrays.toString(someIntegers.toArray()));
        someIntegers = JavacListUtils.addAtIndex(someIntegers, 0, 4);
        checkListEquality(List.of(4, 1, 2, 3), someIntegers);
    }

    @Test
    public void testInsertionToMiddle() {
        List<Integer> someIntegers = List.of(1, 2, 3);
        log.debug("Inserting 4 at index 1 of {}", Arrays.toString(someIntegers.toArray()));
        someIntegers = JavacListUtils.addAtIndex(someIntegers, 1, 4);
        checkListEquality(List.of(1, 4, 2, 3), someIntegers);
    }

    @Test
    public void testInsertionToEnd() {
        List<Integer> someIntegers = List.of(1, 2, 3);
        log.debug("Inserting 4 at index 3 of {}", Arrays.toString(someIntegers.toArray()));
        someIntegers = JavacListUtils.addAtIndex(someIntegers, 3, 4);
        checkListEquality(List.of(1, 2, 3, 4), someIntegers);
    }

    @Test
    public void testSpliceToEmptyList() {
        List<Integer> someMoreIntegers = List.nil();
        log.debug("Inserting [1, 2, 3] at index 0 of []");
        someMoreIntegers = JavacListUtils.addAtIndex(someMoreIntegers, 0, List.of(1, 2, 3));
        checkListEquality(List.of(1, 2, 3), someMoreIntegers);
    }

    @Test
    public void testSpliceToStart() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Inserting [4, 5, 6] at index 0 of {}", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.addAtIndex(someMoreIntegers, 0, List.of(4, 5, 6));
        checkListEquality(List.of(4, 5, 6, 1, 2, 3), someMoreIntegers);
    }

    @Test
    public void testSpliceToMiddle() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Inserting [4, 5, 6] at index 1 of {}", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.addAtIndex(someMoreIntegers, 1, List.of(4, 5, 6));
        checkListEquality(List.of(1, 4, 5, 6, 2, 3), someMoreIntegers);
    }

    @Test
    public void testSpliceToEnd() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Inserting [4, 5, 6] at index 3 of {}", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.addAtIndex(someMoreIntegers, 3, List.of(4, 5, 6));
        checkListEquality(List.of(1, 2, 3, 4, 5, 6), someMoreIntegers);
    }

    @Test
    public void testSetMiddle() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Setting element at index 1 of {} to 4", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.set(someMoreIntegers, 1, 4);
        checkListEquality(List.of(1, 4, 3), someMoreIntegers);
    }

    @Test
    public void testSetStart() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Setting element at index 0 of {} to 4", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.set(someMoreIntegers, 0, 4);
        checkListEquality(List.of(4, 2, 3), someMoreIntegers);
    }

    @Test
    public void testSetEnd() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Setting element at index 4 of {} to 4", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.set(someMoreIntegers, 2, 4);
        checkListEquality(List.of(1, 2, 4), someMoreIntegers);
    }

    @Test
    public void testRemoveStart() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Removing element at index 0 of {}", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.removeAtIndex(someMoreIntegers, 0);
        checkListEquality(List.of(2, 3), someMoreIntegers);
    }

    @Test
    public void testRemoveMiddle() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Removing element at index 1 of {}", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.removeAtIndex(someMoreIntegers, 1);
        checkListEquality(List.of(1, 3), someMoreIntegers);
    }

    @Test
    public void testRemoveMiddleByElement() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Removing '2' from {}", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.removeElement(someMoreIntegers, 2);
        checkListEquality(List.of(1, 3), someMoreIntegers);
    }

    @Test
    public void testRemoveEnd() {
        List<Integer> someMoreIntegers = List.of(1, 2, 3);
        log.debug("Removing '2' from {}", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.removeAtIndex(someMoreIntegers, 2);
        checkListEquality(List.of(1, 2), someMoreIntegers);
    }

    @Test
    public void testAddAndRemove() {
        List<Integer> someMoreIntegers = List.of(1, 2);
        log.debug("Removing '1' then '2' from {}", Arrays.toString(someMoreIntegers.toArray()));
        someMoreIntegers = JavacListUtils.removeElement(someMoreIntegers, 1);
        someMoreIntegers = JavacListUtils.removeElement(someMoreIntegers, 2);
        assertEquals(someMoreIntegers, List.<Integer>nil());
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testNegativeInsert() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(JavacListUtils.INSERT_NEGATIVE);

        List<Integer> list = List.of(1, 2);
        JavacListUtils.addAtIndex(list, -1, 3);
    }

    @Test
    public void testOutOfBoundsInsert() {
        expectedException.expect(IndexOutOfBoundsException.class);
        expectedException.expectMessage(JavacListUtils.INSERT_OUT_OF_BOUNDS);

        List<Integer> list = List.of(1, 2);
        JavacListUtils.addAtIndex(list, 3, 3);
    }

    @Test
    public void testRemoveNonexistent() {
        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage(JavacListUtils.REPLACE_NONEXISTENT);

        List<Integer> list = List.of(1, 2);
        JavacListUtils.replace(list, 3, 1);
    }

    private <T> void checkListEquality(List<T> expected, List<T> actual) {
        log.debug("\n{}\nExpected: {}\n   Found: {}", name.getMethodName(), Arrays.toString(expected.toArray()), Arrays.toString(actual.toArray()));
        Iterator<T> expectedIt = expected.iterator();
        Iterator<T> actualIt = actual.iterator();

        while (expectedIt.hasNext()) {
            T expectedElement = expectedIt.next();
            assertTrue(actualIt.hasNext());
            T actualElement = actualIt.next();
            assertTrue(expectedElement.equals(actualElement));
        }
    }
}
