package lsfusion.gwt.client.base.jsni;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Arrays;

// the order a NativeLinkedSIDMap promises: its keys in the order they were last put - a key put again goes last; a
// removed one leaves the order, and put again goes last. A key put with no value is there (a move waiting with no
// selection mode: GFormController.changeGroupObjectLater). Its keys gone over as the map is: one removed meanwhile -
// the one given too - is not gone over (GFormController.commitDelayedGroupObjectChanges)
public class NativeLinkedSIDMapTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    @Override protected void gwtSetUp() { JsniTestSupport.installMapDelete(); }

    private final HasNativeSID a = () -> "a", b = () -> "b", c = () -> "c";

    private NativeLinkedSIDMap<HasNativeSID, String> abc() {
        NativeLinkedSIDMap<HasNativeSID, String> map = new NativeLinkedSIDMap<>();
        map.put(a, "a1");
        map.put(b, "b1");
        map.put(c, "c1");
        return map;
    }

    private static ArrayList<String> keys(NativeLinkedSIDMap<HasNativeSID, String> map) {
        ArrayList<String> keys = new ArrayList<>();
        map.foreachKey(key -> keys.add(key.getNativeSID()));
        return keys;
    }

    public void testAKeyPutAgainGoesLast() {
        NativeLinkedSIDMap<HasNativeSID, String> map = abc();
        map.put(a, "a2");
        assertEquals(Arrays.asList("b", "c", "a"), keys(map));
        assertEquals("a2", map.get(a));
    }

    public void testARemovedKeyLeavesTheOrder() {
        NativeLinkedSIDMap<HasNativeSID, String> map = abc();
        map.remove(b);
        assertFalse(map.containsKey(b));
        assertNull(map.get(b));
        assertEquals(Arrays.asList("a", "c"), keys(map));
        map.put(b, "b2");
        assertEquals(Arrays.asList("a", "c", "b"), keys(map));
    }

    public void testAKeyPutWithNoValueIsThere() {
        NativeLinkedSIDMap<HasNativeSID, String> map = abc();
        map.put(a, null);
        assertTrue(map.containsKey(a));
        assertNull(map.get(a));
        assertEquals(Arrays.asList("b", "c", "a"), keys(map));
    }

    public void testAKeyRemovedWhileGoingOverIsNotGoneOver() {
        NativeLinkedSIDMap<HasNativeSID, String> map = abc();
        ArrayList<String> gone = new ArrayList<>();
        map.foreachKey(key -> {
            gone.add(key.getNativeSID());
            map.remove(key); // the one given
            if (key == a)
                map.remove(b); // ... and one after it
        });
        assertEquals(Arrays.asList("a", "c"), gone);
        assertTrue(keys(map).isEmpty());
    }
}
