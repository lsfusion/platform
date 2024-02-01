package lsfusion.gwt.client.form.object.table.tree.controller;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GFilterControls;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.grid.user.design.view.GExpandTreeButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButtonGroup;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeGridRecord;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;

public class GTreeGroupController extends GAbstractTableController {

    private final GTreeGroup treeGroup;

    private final GTreeTable tree;
    
    private GExpandTreeButton expandTreeButton;
    private GExpandTreeButton expandTreeCurrentButton;

    public GTreeGroupController(GTreeGroup iTreeGroup, GFormController iFormController, GForm iForm) {
        super(iFormController, iTreeGroup.toolbar, true);
        treeGroup = iTreeGroup;

        initGridView();
        tree = new GTreeTable(iFormController, iForm, this, treeGroup, gridView);

        changeGridView(tree, treeGroup.boxed == null || treeGroup.boxed);
    }

    protected void configureToolbar() {
        initFilters();

        GToolbarButtonGroup expandTreeButtonGroup = new GToolbarButtonGroup();
        expandTreeCurrentButton = new GExpandTreeButton(this, true);
        expandTreeButtonGroup.add(expandTreeCurrentButton);
        expandTreeButton = new GExpandTreeButton(this, false);
        expandTreeButtonGroup.add(expandTreeButton);

        addToToolbar(expandTreeButtonGroup);
    }

    @Override
    public List<GFilter> getFilters() {
        return treeGroup.filters;
    }

    public GFont getFont() {
        return treeGroup.font;
    }

    public void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex) {
        tree.setKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group), fc.expandables.get(group), requestIndex);
    }

    @Override
    public void updateCurrentKey(GGroupObjectValue currentKey) {
        tree.setCurrentKey(currentKey);
    }

    @Override
    public boolean isPropertyShown(GPropertyDraw property) {
        return tree.isPropertyShown(property);
    }

    @Override
    public void focusProperty(GPropertyDraw propertyDraw) {
        tree.focusProperty(propertyDraw);
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateProperty(property, columnKeys, updateKeys, values);
    }

    @Override
    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        return tree.setLoadingValueAt(property, fullCurrentKey, value);
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        tree.removeProperty(property);
    }

    public void update() {
        tree.update();

        boolean isTreeVisible = tree.getColumnCount() > 1;

        GwtClientUtils.setGridVisible(gridView, isTreeVisible);

        if (toolbarView != null)
            GwtClientUtils.setGridVisible(toolbarView, isTreeVisible);

        for (GGroupObject groupObject : treeGroup.groups)
            formController.setFiltersVisible(groupObject, isTreeVisible);

        if (filter != null) {
            filter.update();
            filter.setVisible(isTreeVisible);
        }

        if(expandTreeButton != null) {
            expandTreeButton.update(this);
        }
        if(expandTreeCurrentButton != null) {
            expandTreeCurrentButton.update(this);
        }
    }

    public boolean isCurrentPathExpanded() {
        return tree.isCurrentPathExpanded();
    }

    @Override
    public void updateCellValueElementClasses(GValueElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateCellValueElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellFontValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateCellFontValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateCellBackgroundValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateCellForegroundValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePlaceholderValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updatePlaceholderValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePatternValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updatePatternValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateRegexpValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateRegexpValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateRegexpMessageValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateRegexpMessageValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateTooltipValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateValueTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateValueTooltipValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateImageValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updatePropertyCaptions(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellCaptionElementClasses(GCaptionElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateCaptionElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateLoadings(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updatePropertyFooters(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateReadOnlyValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePropertyComments(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateCellCommentElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateRowBackgroundValues(values);
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        tree.updateRowForegroundValues(values);
    }

    @Override
    public void updateCustomOptionsValues(NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public GGroupObjectValue getSelectedKey() {
        return tree.getSelectedKey();
    }

    @Override
    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet) {
        return tree.changeOrders(groupObject, value, alreadySet);
    }

    // used in filters and user preferences
    @Override
    public GGroupObject getSelectedGroupObject() {
        GTreeGridRecord record = tree.getSelectedRowValue();
        return record != null
                ? record.getGroup()
                : treeGroup.groups.get(0);
    }

    @Override
    public List<GPropertyDraw> getGroupObjectProperties() {
        GGroupObject currentGroupObject = getSelectedGroupObject();

        ArrayList<GPropertyDraw> properties = new ArrayList<>();
        for (GPropertyDraw property : formController.getPropertyDraws()) {
            if (currentGroupObject != null && currentGroupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }

        return properties;
    }

    @Override
    public GPropertyDraw getSelectedFilterProperty() {
        return tree.getSelectedFilterProperty();
    }

    @Override
    public GGroupObjectValue getSelectedColumnKey() {
        return GGroupObjectValue.EMPTY; // пока не поддерживаются группы в колонки
    }

    @Override
    public PValue getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return tree.getSelectedValue(property);
    }

    @Override
    public List<Pair<Column, String>> getFilterColumns() {
        return tree.getFilterColumns(getSelectedGroupObject());
    }

    @Override
    public GContainer getFiltersContainer() {
        return treeGroup.filtersContainer;
    }

    @Override
    public GFilterControls getFilterControls() {
        return treeGroup.filterControls;
    }

    public boolean focusFirstWidget(FocusUtils.Reason reason) {
        if (isShowing(tree.getWidget())) {
            tree.focus(reason);
            return true;
        }

        return false;
    }

    @Override
    public GComponent getGridComponent() {
        return treeGroup;
    }

    @Override
    protected long changeFilter(ArrayList<GPropertyFilter> conditions) {
        return formController.changeFilter(treeGroup, conditions);
    }
    
    public boolean isExpandOnClick() {
        return treeGroup.expandOnClick;
    }

    public void fireExpandNodeRecursive(boolean current) {
        tree.fireExpandNodeRecursive(current, true);
    }

    public void fireCollapseNodeRecursive(boolean current) {
        tree.fireExpandNodeRecursive(current, false);
    }
}
