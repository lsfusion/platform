package lsfusion.gwt.client;

import com.google.gwt.junit.client.GWTTestCase;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.form.filter.user.GFilterValueDTO;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyReaderDTO;
import lsfusion.gwt.client.form.view.Column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

// What the client takes of the user orders and filters the server reports for a group: the orders in their priority
// order, by the property and its column key, true - ascending - no orders too, as an empty list -; the filters as they
// are
public class GFormChangesTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    @Override protected void gwtSetUp() { JsniTestSupport.installMapDelete(); }

    private GForm form;
    private GGroupObject group;
    private GPropertyDraw name, price;

    private void setUpForm() {
        form = new GForm();
        group = new GGroupObject();
        group.ID = 10; group.nativeSID = "g10"; group.sID = "items";
        form.groupObjects.add(group);
        name = property(20, "name");
        price = property(21, "price");
    }

    private GPropertyDraw property(int id, String sID) {
        GPropertyDraw property = new GPropertyDraw();
        property.ID = id; property.nativeSID = "p" + id; property.sID = sID;
        property.groupObject = group;
        form.propertyDraws.add(property);
        return property;
    }

    // the changes of a request that changed nothing
    @SuppressWarnings("unchecked")
    private static GFormChangesDTO noChanges() {
        GFormChangesDTO dto = new GFormChangesDTO();
        dto.objectsGroupIds = new int[0]; dto.objects = new GGroupObjectValue[0];
        dto.gridObjectsGroupIds = new int[0]; dto.gridObjects = new ArrayList[0];
        dto.parentObjectsGroupIds = new int[0]; dto.parentObjects = new ArrayList[0];
        dto.expandablesGroupIds = new int[0]; dto.expandableKeys = new GGroupObjectValue[0][]; dto.expandableValues = new Integer[0][];
        dto.properties = new GPropertyReaderDTO[0]; dto.propertiesValueKeys = new GGroupObjectValue[0][]; dto.propertiesValueValues = new Serializable[0][];
        dto.dropPropertiesIds = new int[0];
        dto.updateStateObjectsGroupIds = new int[0]; dto.updateStateObjectsGroupValues = new boolean[0];
        dto.activateTabsIds = new int[0]; dto.activatePropsIds = new int[0];
        dto.collapseContainerIds = new int[0]; dto.expandContainerIds = new int[0];
        dto.userOrdersGroupIds = new int[0]; dto.userOrdersPropertyIds = new int[0][]; dto.userOrdersColumnKeys = new GGroupObjectValue[0][]; dto.userOrdersAscending = new boolean[0][];
        dto.userFiltersGroupIds = new int[0]; dto.userFilters = new GPropertyFilterDTO[0][];
        return dto;
    }

    // two columns of one property - a property in columns - are two orders
    public void testOrdersKeepTheirPriorityAndColumns() {
        setUpForm();
        GGroupObjectValue first = new GGroupObjectValue(30, 1), second = new GGroupObjectValue(30, 2);
        GFormChangesDTO dto = noChanges();
        dto.userOrdersGroupIds = new int[] {10};
        dto.userOrdersPropertyIds = new int[][] {{21, 20, 21}};
        dto.userOrdersColumnKeys = new GGroupObjectValue[][] {{second, GGroupObjectValue.EMPTY, first}};
        dto.userOrdersAscending = new boolean[][] {{false, true, true}};

        LinkedHashMap<Column, Boolean> orders = GFormChanges.remap(form, dto).userOrders.get(group);
        assertEquals(Arrays.asList(new Column(price, second), new Column(name, GGroupObjectValue.EMPTY), new Column(price, first)),
                new ArrayList<>(orders.keySet()));
        assertEquals(Arrays.asList(false, true, true), new ArrayList<>(orders.values()));
    }

    public void testNoOrdersAreReportedAsAnEmptyList() {
        setUpForm();
        GFormChangesDTO dto = noChanges();
        dto.userOrdersGroupIds = new int[] {10};
        dto.userOrdersPropertyIds = new int[][] {{}};
        dto.userOrdersColumnKeys = new GGroupObjectValue[][] {{}};
        dto.userOrdersAscending = new boolean[][] {{}};

        GFormChanges changes = GFormChanges.remap(form, dto);
        assertTrue(changes.userOrders.containsKey(group));
        assertTrue(changes.userOrders.get(group).isEmpty());
    }

    public void testFiltersAreTakenAsTheyAre() {
        setUpForm();
        GPropertyFilterDTO filter = new GPropertyFilterDTO();
        filter.propertyID = 21; filter.columnKey = GGroupObjectValue.EMPTY; filter.filterValue = new GFilterValueDTO(2);
        filter.compareByte = 1; filter.negation = false; filter.junction = true;
        GFormChangesDTO dto = noChanges();
        dto.userFiltersGroupIds = new int[] {10};
        dto.userFilters = new GPropertyFilterDTO[][] {{filter}};

        assertEquals(Collections.singletonList(filter), GFormChanges.remap(form, dto).userFilters.get(group));
    }

    public void testAGroupNotReportedHasNoEntry() {
        setUpForm();
        GFormChanges changes = GFormChanges.remap(form, noChanges());
        assertFalse(changes.userOrders.containsKey(group));
        assertFalse(changes.userFilters.containsKey(group));
    }
}
