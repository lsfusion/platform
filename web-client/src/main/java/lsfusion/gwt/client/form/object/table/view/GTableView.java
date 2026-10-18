package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableComponent;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

import java.util.ArrayList;

// THE VIEW the platform draws a TABLE's rows in (GPlatformTableController.view): a grid's - the list view it shows
// (GListView) -, or a tree (GTreeTable). What the form sends for the table's rows and properties goes to it, a
// property's attribute with the property; what a view does not draw it leaves (the defaults)
public interface GTableView extends TableComponent {

    // keys
    void setCurrentKey(GGroupObjectValue currentKey);
    GGroupObjectValue getSelectedKey(); // null for none
    int getSelectedRow();
    default void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
    }

    // columns
    void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values); // add or update
    void removeProperty(GPropertyDraw property);
    boolean containsProperty(GPropertyDraw property);
    void updateOrders(); // the orders the form has for the group changed - a view that shows them reads them there
    void focusProperty(GPropertyDraw propertyDraw);
    // a change of a cell sent, loading until the answer comes (GPropertyController.setLoadingValueAt): its key and its
    // value before - null where the view has no cell for it
    default Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        return null;
    }

    // rows
    void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values);
    void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values);
    default void updateRowSelectValues(NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
    }
    default void updateCustomOptionsValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        //implemented only in GCustom
    }

    // cells
    void updateCellGridElementClasses(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateCellValueElementClasses(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateCellFontValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateCellBackgroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateCellForegroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updatePlaceholderValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updatePatternValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateRegexpValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateRegexpMessageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateTooltipValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateValueTooltipValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updatePropertyCustomOptionsValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    // the keys a property gives itself (a design's changeKey / changeMouse given by a property): the form binds them
    // (GExtraPropReader.update), a view only shows them - by default, nothing
    default void updateChangeKeyValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
    }
    default void updateChangeMouseValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values) {
    }
    void updateImageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updatePropertyCaptions(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateCaptionElementClasses(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateFooterElementClasses(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateLoadings(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updatePropertyFooters(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    void updateReadOnlyValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, PValue> values);
    default void updateShowIfValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
    }
    default void updateLastValues(GPropertyDraw property, int index, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    // focus
    void focus(FocusUtils.Reason reason);

    default void onClear(){}
    // a property's bindings where the view draws it (GPlatformTableController.getBindingWidget): the widget they are
    // taken with, null where the view takes none - only the grid takes them -, and what one does
    default Widget getBindingWidget(GPropertyDraw property) {
        return null;
    }
    default void onBinding(GPropertyDraw property, Event event) {
    }

    // the view is dropped for good (the form is closing, or the table view is being switched); everything this view
    // registered in a page-lifetime structure (MainFrame's color-theme listener list) has to be unregistered here
    default void destroy(){}

    default boolean isDefaultBoxed() { return true; }
}
