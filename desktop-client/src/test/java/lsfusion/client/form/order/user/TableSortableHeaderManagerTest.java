package lsfusion.client.form.order.user;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.view.Column;
import lsfusion.interop.form.order.user.Order;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

// What the desktop header shows of the sortings and asks for on a click - as GGridSortableHeaderManagerTest holds for
// the web header: the orders the form has for the group of each column, read as it draws them; a click asks for the
// orders of its column's group changed by it, showing nothing itself, and nothing on a column the design says not to
// sort by (noSort)
public class TableSortableHeaderManagerTest {

    private ClientGroupObject items, lines;
    private Column price, name, sku, line;
    // the orders the form has, by group
    private Map<ClientGroupObject, LinkedHashMap<Column, Boolean>> formOrders;
    // the group and the orders a click asked for
    private ClientGroupObject askedGroup;
    private LinkedHashMap<Column, Boolean> asked;
    private TableSortableHeaderManager manager;

    @Before
    public void setUp() {
        items = new ClientGroupObject(); lines = new ClientGroupObject();
        price = column(items, false);
        name = column(items, false);
        sku = column(items, true); // a column the design says not to sort by
        line = column(lines, false); // a tree's header has columns of another group too
        List<Column> columns = Arrays.asList(price, name, sku, line);
        formOrders = new HashMap<>();
        askedGroup = null; asked = null;
        manager = new TableSortableHeaderManager(null) {
            protected LinkedHashMap<Column, Boolean> getOrders(ClientGroupObject group) {
                LinkedHashMap<Column, Boolean> orders = formOrders.get(group);
                return orders != null ? orders : new LinkedHashMap<>();
            }
            protected void changeOrders(ClientGroupObject group, LinkedHashMap<Column, Boolean> orders) {
                askedGroup = group; asked = orders;
            }
            protected Column getColumn(int column) {
                return column >= 0 && column < columns.size() ? columns.get(column) : null;
            }
        };
    }

    private static Column column(ClientGroupObject group, boolean noSort) {
        ClientPropertyDraw property = new ClientPropertyDraw();
        property.groupObject = group;
        property.noSort = noSort;
        return new Column(property, ClientGroupObjectValue.EMPTY);
    }

    private static LinkedHashMap<Column, Boolean> orders(Object... columnsAndAscending) {
        LinkedHashMap<Column, Boolean> orders = new LinkedHashMap<>();
        for (int i = 0; i < columnsAndAscending.length; i += 2)
            orders.put((Column) columnsAndAscending[i], (Boolean) columnsAndAscending[i + 1]);
        return orders;
    }

    @Test
    public void theFormsSortingsAreShown() {
        formOrders.put(items, orders(price, true, name, false));
        assertEquals(Boolean.TRUE, manager.getSortDirection(0));
        assertEquals(Boolean.FALSE, manager.getSortDirection(1));
        assertNull(manager.getSortDirection(2));
        assertNull(manager.getSortDirection(4)); // no column there
        assertNull(asked);
    }

    @Test
    public void aSortingTheFormHasOnANoSortColumnIsShown() {
        formOrders.put(items, orders(sku, false));
        assertEquals(Boolean.FALSE, manager.getSortDirection(2));
    }

    @Test
    public void aClickAsksForItsSortingsAndDoesNotShowThemItself() {
        formOrders.put(items, orders(price, true));
        manager.changeOrder(name, Order.REPLACE);
        assertSame(items, askedGroup);
        assertEquals(orders(name, true), asked);
        assertEquals(Boolean.TRUE, manager.getSortDirection(0));
        assertNull(manager.getSortDirection(1));
    }

    @Test
    public void aClickOnANoSortColumnAsksForNothing() {
        manager.changeOrder(sku, Order.REPLACE);
        assertNull(asked);
        manager.changeOrder(price, Order.REPLACE);
        assertEquals(orders(price, true), asked);
    }

    @Test
    public void addTurnAndRemove() {
        formOrders.put(items, orders(price, true));
        manager.changeOrder(name, Order.ADD);
        assertEquals(new ArrayList<>(orders(price, true, name, true).entrySet()), new ArrayList<>(asked.entrySet()));
        manager.changeOrder(price, Order.DIR);
        assertEquals(orders(price, false), asked);
        manager.changeOrder(price, Order.REMOVE);
        assertEquals(orders(), asked);
    }

    // a column of another group - a tree's header has the columns of all its groups - asks for that group's orders
    @Test
    public void aClickAsksForTheOrdersOfItsColumnsGroup() {
        formOrders.put(items, orders(price, true));
        manager.changeOrder(line, Order.ADD);
        assertSame(lines, askedGroup);
        assertEquals(orders(line, true), asked);
        assertEquals(Boolean.TRUE, manager.getSortDirection(0));
    }
}
