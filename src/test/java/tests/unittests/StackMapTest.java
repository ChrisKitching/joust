package tests.unittests;

import joust.utils.LogUtils;
import joust.utils.map.StackMap;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;

import java.util.logging.Logger;
import static org.junit.Assert.*;

/**
 * Unit tests for the StackMap class.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class StackMapTest {
    @Test
    public void testInsertion() {
        StackMap<Integer, String> map = new StackMap<>();
        map.pushMap();
        map.put(1, "a");
        map.put(5, "e");

        map.pushMap();

        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(5));
        assertFalse(map.containsKey(7));

        assertFalse(map.isEmpty());
        assertTrue(map.containsValue("a"));
        assertTrue(map.containsValue("e"));
        assertFalse(map.containsValue("h"));
        assertTrue(map.size() == 2);

        assertTrue("a".equals(map.get(1)));
        assertTrue("e".equals(map.get(5)));
    }

    @Test
    public void testComplexInsertion() {
        StackMap<Integer, String> map = new StackMap<>();
        map.pushMap();
        map.put(1, "a");
        map.put(5, "e");

        map.pushMap();

        map.put(2, "hi");
        map.put(8, "turtles");

        assertTrue(map.size() == 4);
        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(8));
        assertFalse(map.containsKey(7));

        assertFalse(map.isEmpty());
        assertTrue(map.containsValue("a"));
        assertTrue(map.containsValue("turtles"));
        assertFalse(map.containsValue("h"));

        assertTrue("a".equals(map.get(1)));
        assertTrue("e".equals(map.get(5)));
        assertTrue("hi".equals(map.get(2)));
        assertTrue("turtles".equals(map.get(8)));
    }

    @Test
    public void testPopping() {
        StackMap<Integer, String> map = new StackMap<>();
        map.pushMap();
        map.put(1, "a");
        map.put(5, "e");

        map.pushMap();

        map.put(2, "hi");
        map.put(8, "turtles");

        map.popMap();

        assertTrue(map.size() == 2);
        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(5));
        assertFalse(map.containsKey(7));

        assertFalse(map.isEmpty());
        assertTrue(map.containsValue("a"));
        assertTrue(map.containsValue("e"));
        assertFalse(map.containsValue("h"));

        assertTrue("a".equals(map.get(1)));
        assertTrue("e".equals(map.get(5)));

        assertFalse(map.containsKey(2));
        assertFalse(map.containsValue("turtles"));
        assertTrue(map.get(8) == null);

        map.popMap();

        assertTrue(map.size() == 0);
        assertTrue(map.isEmpty());
    }
}
