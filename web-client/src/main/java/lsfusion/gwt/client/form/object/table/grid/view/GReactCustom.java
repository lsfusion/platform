package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GGroupController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.property.GExtraPropertyReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.GReactFormData;
import lsfusion.gwt.client.form.design.view.ReactBaseComponentView;
import lsfusion.gwt.client.form.object.panel.controller.GPlatformRowsPanelController;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GPlatformGridController;
import lsfusion.gwt.client.form.property.cell.view.CustomCellRenderer;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.List;

// the CUSTOM view of a group's rows whose render function is a React component (ReactRoot.isReactView): what GCustom
// is to a classic render function. The component is given what a React container that draws the group's rows is
// given of the group - its node as `data`, its member as `controller` -, kept as that container's is, by the
// projection's own parts and entries (`state`). The view keeps nothing of the group itself: what the grid tells it,
// as it tells any of its views, it hands on to them, each attribute to the entry of its property by the reader it
// came from; and what it is asked - by the form, by the grid's toolbar, by its filters - it answers from them
public final class GReactCustom extends GPagedListView {
    private final GReactFormData.GroupViewState state;
    private final ReactRoot root;
    // what draws the properties the platform draws in each row, where the group has any (GReactFormData.GroupViewState)
    private GPlatformRowsPanelController rowsPanel;

    public GReactCustom(GFormController form, GPlatformGridController grid, TableContainer tableContainer, String name) {
        super(form, grid, tableContainer);
        state = form.addReactGroupView(grid.groupObject.grid, this::publish, rows -> rowsPanel = form.addRowsPanelController(grid.groupObject, rows));
        root = new ReactRoot(name, state.controller, ReactBaseComponentView.rowsPlacement(form, state, name));
        root.updateData(state.getData());
        Element element = getDrawElement();
        GwtClientUtils.setZeroZIndex(element);
        CustomCellRenderer.setCustomElement(element);
        // drawn while the view is attached - a grid switched to another view detaches it -, and, in a form whose size
        // is fixed as it is shown, before the form is measured: a grid in CUSTOM view takes the size of what it draws.
        // Where a React view places the grid, in that view's tree (ReactRoot.settle)
        form.getFormLayout().mountReactView(root, this, element);
    }
    private void publish(JavaScriptObject data) {
        root.updateData(data);
    }
    @Override
    public void destroy() {
        if (rowsPanel != null)
            form.removeRowsPanelController(grid.groupObject);
    }

    private GGroupController getRows() {
        return state.getTableController().getGroupController(grid.groupObject);
    }
    private GPropertyController getEntry(GPropertyDraw property) {
        return state.getPropertyController(property);
    }

    // ===== what the view is told of the group: its rows, the current one, a row added or removed before the answer,
    // its sortings and its view filters as the form has them, and what a reader of the group brings
    @Override
    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        getRows().updateKeys(grid.groupObject, keys, null, 0);
    }
    @Override
    public void setCurrentKey(GGroupObjectValue currentKey) {
        getRows().updateCurrentKey(currentKey);
    }
    @Override
    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
        getRows().modifyGroupObject(key, add, position);
    }
    @Override
    public void updateOrders() {
        state.getTableController().getUserOrdersController().updateOrders(grid.groupObject, form.getOrders(grid.groupObject));
    }
    @Override
    public void changeViewFilters(List<GPropertyFilter> conditions) {
        getRows().changeViewFilters(conditions);
    }
    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        getRows().updateAttribute(grid.groupObject.rowBackgroundReader, values, false);
    }
    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        getRows().updateAttribute(grid.groupObject.rowForegroundReader, values, false);
    }
    @Override
    public void updateRowSelectValues(NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        getRows().updateAttribute(grid.groupObject.rowSelectReader, values, updateKeys);
    }
    @Override
    public void updateCustomOptionsValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        getRows().updateAttribute(grid.groupObject.customOptionsReader, values, false);
    }

    // ===== ... of a property: its values, its drop, a change of a cell sent
    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
        getEntry(property).updateValue(property, values, updateKeys);
    }
    @Override
    public void removeProperty(GPropertyDraw property) {
        getEntry(property).dropProperty(property);
    }
    @Override
    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        return getEntry(property).setLoadingValueAt(property, fullCurrentKey, value);
    }
    // ... and what a reader of it brings: told with the property alone, so handed on by the reader it came from,
    // which says what kind it is (GExtraPropertyReader.update) - whole, as the form sends every one but its loading,
    // which is the cells whose changes are answered
    private void update(GPropertyDraw property, GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        reader.update(getEntry(property), values, false);
    }
    @Override
    public void updateLoadings(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        property.loadingReader.update(getEntry(property), values, true);
    }
    @Override
    public void updatePropertyCaptions(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.captionReader, values);
    }
    @Override
    public void updateImageValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.imageReader, values);
    }
    @Override
    public void updatePropertyFooters(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.footerReader, values);
    }
    @Override
    public void updatePropertyComments(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.commentReader, values);
    }
    @Override
    public void updateReadOnlyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.readOnlyReader, values);
    }
    @Override
    public void updateCellBackgroundValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.backgroundReader, values);
    }
    @Override
    public void updateCellForegroundValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.foregroundReader, values);
    }
    @Override
    public void updatePlaceholderValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.placeholderReader, values);
    }
    @Override
    public void updatePatternValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.patternReader, values);
    }
    @Override
    public void updateRegexpValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.regexpReader, values);
    }
    @Override
    public void updateRegexpMessageValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.regexpMessageReader, values);
    }
    @Override
    public void updateTooltipValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.tooltipReader, values);
    }
    @Override
    public void updateValueTooltipValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.valueTooltipReader, values);
    }
    @Override
    public void updatePropertyCustomOptionsValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.propertyCustomOptionsReader, values);
    }
    @Override
    public void updateDefaultValueValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.defaultValueReader, values);
    }
    // ... what only an editor the platform draws in a row is drawn with (GroupViewState): its classes, its font, the
    // keys it shows
    @Override
    public void updateCellGridElementClasses(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.gridElementClassReader, values);
    }
    @Override
    public void updateCellValueElementClasses(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.valueElementClassReader, values);
    }
    @Override
    public void updateCaptionElementClasses(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.captionElementClassReader, values);
    }
    @Override
    public void updateFooterElementClasses(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.footerElementClassReader, values);
    }
    @Override
    public void updateCellFontValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.fontReader, values);
    }
    @Override
    public void updateChangeKeyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.changeKeyReader, values);
    }
    @Override
    public void updateChangeMouseValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values) {
        update(property, property.changeMouseReader, values);
    }
    // ... and nothing to draw: the component draws from what `state` publishes
    @Override
    public void update(Boolean updateState) {
        updateShown(updateState);
    }

    // ===== what the view is asked: by the form, of the group's rows and of a property's keys and focus - an editor
    // the platform draws in the current row takes the focus, the table where the component draws the property ...
    @Override
    public GGroupObjectValue getCurrentKey() {
        return getRows().getSelectedKey();
    }
    @Override
    public int getCurrentRow() {
        return getRow(getCurrentKey());
    }
    @Override
    public int getRow(GGroupObjectValue key) {
        return getRows().getRow(key);
    }
    @Override
    public boolean containsProperty(GPropertyDraw property) {
        return getEntry(property).isPropertyShown(property);
    }
    @Override
    public Widget getBindingWidget(GPropertyDraw property) {
        return getEntry(property).getBindingWidget(property);
    }
    @Override
    public void onBinding(GPropertyDraw property, Event event) {
        getEntry(property).onBinding(property, event);
    }
    @Override
    public void focusProperty(GPropertyDraw property) {
        if (property.explicitCustom)
            getEntry(property).focusProperty(property);
        else
            super.focusProperty(property);
    }
    // ... and by the grid, its toolbar and its filters, of its rows and columns
    @Override
    protected int getRowCount() {
        return state.getRowCount();
    }
    @Override
    public boolean isNoColumns() {
        return !state.hasShownProperty();
    }
    @Override
    public GPropertyDraw getCurrentProperty() {
        return state.getTableController().getSelectedFilterProperty();
    }
    @Override
    public GGroupObjectValue getCurrentColumnKey() {
        return GGroupObjectValue.EMPTY;
    }
    @Override
    public List<Pair<Column, String>> getFilterColumns() {
        return state.getTableController().getFilterColumns();
    }
    @Override
    public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return state.getTableController().getSelectedValue(property, columnKey);
    }

    // the form's keys and mouse bindings over what the component draws, which has no cell of the platform's
    @Override
    public void onBrowserEvent(Element target, EventHandler eventHandler) {
        form.onPropertyBrowserEvent(eventHandler, null, false, getTableDataFocusElement(),
                handler -> {}, handler -> {}, handler -> {}, handler -> {}, handler -> {}, false, true, true);
    }

    @Override
    protected Element getRendererAreaElement() {
        return getElement();
    }
}
