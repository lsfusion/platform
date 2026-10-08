package lsfusion.gwt.client.form.order.user;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.LinkedHashMap;

// What a grid's header shows of the sortings: the form gives them - as a click asked for them or as the server reported
// them -, and the header takes them as they are, in priority order, sending nothing; a click asks for the sortings it
// gives, which it shows once the form gives them
public class GGridSortableHeaderManagerTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    private GGridSortableHeaderManager<String> manager;
    // the sortings a click asked for
    private LinkedHashMap<String, Boolean> asked;

    @Override protected void gwtSetUp() {
        asked = null;
        manager = new GGridSortableHeaderManager<String>(null, false) {
            protected void ordersChanged(String columnKey, LinkedHashMap<String, Boolean> orders) {
                asked = orders;
            }
            protected String getColumnKey(int column) {
                throw new UnsupportedOperationException();
            }
            protected boolean noSort(String columnKey) { // `sku`: a column the design says not to sort by
                return "sku".equals(columnKey);
            }
        };
    }

    private static LinkedHashMap<String, Boolean> orders(Object... columnsAndAscending) {
        LinkedHashMap<String, Boolean> orders = new LinkedHashMap<>();
        for (int i = 0; i < columnsAndAscending.length; i += 2)
            orders.put((String) columnsAndAscending[i], (Boolean) columnsAndAscending[i + 1]);
        return orders;
    }

    public void testReportedSortingsAreShown() {
        assertTrue(manager.updateOrders(orders("price", true, "name", false)));
        assertEquals(orders("price", true, "name", false), manager.getOrderDirections());
        assertNull(asked);
    }

    public void testTheSameSortingsReportedAgainChangeNothing() {
        manager.updateOrders(orders("price", true, "name", false));
        assertFalse(manager.updateOrders(orders("price", true, "name", false)));
    }

    public void testAnotherPriorityIsAnotherSorting() {
        manager.updateOrders(orders("price", true, "name", false));
        assertTrue(manager.updateOrders(orders("name", false, "price", true)));
        assertEquals(new ArrayList<>(orders("name", false, "price", true).entrySet()), new ArrayList<>(manager.getOrderDirections().entrySet()));
    }

    public void testAClickAsksForItsSortingsAndDoesNotShowThemItself() {
        manager.updateOrders(orders("price", true));
        manager.changeOrder("name", GOrder.REPLACE);
        assertEquals(orders("name", true), asked);
        assertEquals(orders("price", true), manager.getOrderDirections());
    }

    // a click on a column the design says not to sort by asks for nothing - the column key telling it of each kind of
    // table, a grid's map or a tree's property alike (noSort)
    public void testAClickOnANoSortColumnAsksForNothing() {
        manager.changeOrder("sku", GOrder.REPLACE);
        assertNull(asked);
        manager.changeOrder("price", GOrder.REPLACE);
        assertEquals(orders("price", true), asked);
    }

    public void testAClickOnTheSortedColumnTurnsIt() {
        manager.updateOrders(orders("price", true));
        manager.changeOrder("price", GOrder.REPLACE);
        assertEquals(orders("price", false), asked);
    }

    public void testCtrlAndShiftClicksAddTurnAndRemove() {
        manager.updateOrders(orders("price", true));
        manager.changeOrder("name", GOrder.ADD);
        assertEquals(new ArrayList<>(orders("price", true, "name", true).entrySet()), new ArrayList<>(asked.entrySet()));
        manager.changeOrder("price", GOrder.DIR);
        assertEquals(orders("price", false), asked);
        manager.changeOrder("price", GOrder.REMOVE);
        assertEquals(orders(), asked);
    }
}
