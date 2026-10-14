package lsfusion.gwt.client.form.order.user;

import com.google.gwt.junit.client.GWTTestCase;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// What a grid's header shows of the sortings: the orders the form has for the group of each column, read as it draws
// them - so a header made after the orders were set shows them too; a click asks for the orders of its column's group
// changed by it, showing nothing itself, and a click on a column the design says not to sort by asks for nothing
public class GGridSortableHeaderManagerTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    private GGroupObject items, lines;
    private Column price, name, sku, line;
    private List<Column> columns;
    // the orders the form has, by group
    private Map<GGroupObject, LinkedHashMap<Column, Boolean>> formOrders;
    // the group and the orders a click asked for
    private GGroupObject askedGroup;
    private LinkedHashMap<Column, Boolean> asked;
    private GGridSortableHeaderManager manager;

    @Override protected void gwtSetUp() {
        items = group(10); lines = group(11);
        price = column(items, 20, false);
        name = column(items, 21, false);
        sku = column(items, 22, true); // a column the design says not to sort by
        line = column(lines, 23, false); // a tree's header has columns of another group too
        columns = Arrays.asList(price, name, sku, line);
        formOrders = new HashMap<>();
        askedGroup = null; asked = null;
        manager = new GGridSortableHeaderManager(false) {
            protected LinkedHashMap<Column, Boolean> getOrders(GGroupObject group) {
                LinkedHashMap<Column, Boolean> orders = formOrders.get(group);
                return orders != null ? orders : new LinkedHashMap<>();
            }
            protected void changeOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
                askedGroup = group; asked = orders;
            }
            protected Column getColumn(int column) {
                return column >= 0 && column < columns.size() ? columns.get(column) : null;
            }
        };
    }

    private static GGroupObject group(int id) {
        GGroupObject group = new GGroupObject();
        group.ID = id; group.nativeSID = "g" + id; group.sID = "g" + id;
        return group;
    }

    private static Column column(GGroupObject group, int id, boolean noSort) {
        GPropertyDraw property = new GPropertyDraw();
        property.ID = id; property.nativeSID = "p" + id; property.sID = "p" + id;
        property.groupObject = group;
        property.noSort = noSort;
        return new Column(property, GGroupObjectValue.EMPTY);
    }

    private static LinkedHashMap<Column, Boolean> orders(Object... columnsAndAscending) {
        LinkedHashMap<Column, Boolean> orders = new LinkedHashMap<>();
        for (int i = 0; i < columnsAndAscending.length; i += 2)
            orders.put((Column) columnsAndAscending[i], (Boolean) columnsAndAscending[i + 1]);
        return orders;
    }

    private static List<Map.Entry<Column, Boolean>> inOrder(LinkedHashMap<Column, Boolean> orders) {
        return new ArrayList<>(orders.entrySet());
    }

    public void testTheFormsSortingsAreShown() {
        formOrders.put(items, orders(price, true, name, false));
        assertEquals(Boolean.TRUE, manager.getSortDirection(0));
        assertEquals(Boolean.FALSE, manager.getSortDirection(1));
        assertNull(manager.getSortDirection(2));
        assertNull(manager.getSortDirection(4)); // no column there
        assertNull(asked);
    }

    // a sorting on a column the user may not sort by is shown as the form has it
    public void testASortingOnANoSortColumnIsShown() {
        formOrders.put(items, orders(sku, false));
        assertEquals(Boolean.FALSE, manager.getSortDirection(2));
    }

    public void testAClickAsksForItsSortingsAndDoesNotShowThemItself() {
        formOrders.put(items, orders(price, true));
        manager.changeOrder(name, GOrder.REPLACE);
        assertSame(items, askedGroup);
        assertEquals(orders(name, true), asked);
        assertEquals(Boolean.TRUE, manager.getSortDirection(0));
        assertNull(manager.getSortDirection(1));
    }

    public void testAClickOnANoSortColumnAsksForNothing() {
        manager.changeOrder(sku, GOrder.REPLACE);
        assertNull(asked);
        manager.headerClicked(2, false, false);
        assertNull(asked);
        manager.changeOrder(price, GOrder.REPLACE);
        assertEquals(orders(price, true), asked);
    }

    public void testAClickOnTheSortedColumnTurnsIt() {
        formOrders.put(items, orders(price, true));
        manager.headerClicked(0, false, false);
        assertEquals(orders(price, false), asked);
    }

    public void testCtrlAndShiftClicksAddTurnAndRemove() {
        formOrders.put(items, orders(price, true));
        manager.headerClicked(1, true, false);
        assertEquals(inOrder(orders(price, true, name, true)), inOrder(asked));
        manager.headerClicked(0, true, false);
        assertEquals(orders(price, false), asked);
        manager.headerClicked(0, false, true);
        assertEquals(orders(), asked);
    }

    // a column of another group - a tree's header has the columns of all its groups - asks for that group's orders,
    // and leaves the others' as they are
    public void testAClickAsksForTheOrdersOfItsColumnsGroup() {
        formOrders.put(items, orders(price, true));
        formOrders.put(lines, orders());
        manager.headerClicked(3, true, false);
        assertSame(lines, askedGroup);
        assertEquals(orders(line, true), asked);
        assertEquals(Boolean.TRUE, manager.getSortDirection(0));
    }
}
