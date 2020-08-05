package lsfusion.gwt.client.form.object.table.tree.controller;

import com.google.gwt.user.client.ui.Panel;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.table.view.GridPanel;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.grid.user.design.view.GExpandTreeButton;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeGridRecord;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.view.Column;

import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;

public class GTreeGroupController extends GAbstractTableController {

    private final GTreeGroup treeGroup;

    private final Panel treeView;

    private final GTreeTable tree;
    
    private final GExpandTreeButton expandTreeButton;
    private final GExpandTreeButton expandTreeCurrentButton;

    public GTreeGroupController(GTreeGroup iTreeGroup, GFormController iFormController, GForm iForm) {
        super(iFormController, iTreeGroup.toolbar, true);
        treeGroup = iTreeGroup;

        tree = new GTreeTable(iFormController, iForm, this, treeGroup, treeGroup.autoSize);

        ResizableSimplePanel resizePanel = new ResizableSimplePanel();
        resizePanel.setStyleName("gridResizePanel");
        resizePanel.setFillWidget(tree);

        if(treeGroup.autoSize) { // убираем default'ый minHeight
            resizePanel.getElement().getStyle().setProperty("minHeight", "0px");
            resizePanel.getElement().getStyle().setProperty("minWidth", "0px");
        }

        treeView = new GridPanel(resizePanel, resizePanel);

        getFormLayout().addBaseComponent(treeGroup, treeView, getDefaultFocusReceiver());

        addFilterButton();

        addToolbarSeparator();
        
        expandTreeCurrentButton = new GExpandTreeButton(this, true);
        addToToolbar(expandTreeCurrentButton);
        expandTreeButton = new GExpandTreeButton(this, false);
        addToToolbar(expandTreeButton);
    }
    
    public GFont getFont() {
        return treeGroup.font;
    }

    public void processFormChanges(GFormChanges fc) {
        for (GGroupObject group : treeGroup.groups) {
            if (fc.gridObjects.containsKey(group)) {
                tree.setKeys(group, fc.gridObjects.get(group), fc.parentObjects.get(group), fc.expandables.get(group));
            }

            for (GPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == group) {
                    removeProperty(group, property);
                }
            }

            fc.properties.foreachEntry((key, value) -> {
                if (key instanceof GPropertyDraw) {
                    GPropertyDraw property = (GPropertyDraw) key;
                    if (property.groupObject == group)
                        updateProperty(group, property, GGroupObjectValue.SINGLE_EMPTY_KEY_LIST, fc.updateProperties.contains(property), value);
                }
            });

            fc.properties.foreachEntry((key, value) -> {
                if (!(key instanceof GPropertyDraw)) {
                    if (formController.getGroupObject(key.getGroupObjectID()) == group) {
                        key.update(this, value, false);
                    }
                }
            });

            if (fc.objects.containsKey(group)) {
                tree.setCurrentKey(fc.objects.get(group));
            }
        }
        update();
    }

    private void removeProperty(GGroupObject group, GPropertyDraw property) {
        if(property.grid)
            tree.removeProperty(group, property);
        else
            panel.removeProperty(property);
    }

    private void updateProperty(GGroupObject group, GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        if (property.grid) {
            tree.updateProperty(group, property, columnKeys, updateKeys, values);
        } else {
            panel.updateProperty(property, columnKeys, updateKeys, values);
        }
    }

    private void update() {
        tree.update();

        tree.restoreVisualState();

        boolean isTreeVisible = tree.getColumnCount() > 1;

        treeView.setVisible(isTreeVisible);

        if (toolbarView != null)
            toolbarView.setVisible(isTreeVisible);

        for (GGroupObject groupObject : treeGroup.groups)
            formController.setFiltersVisible(groupObject, isTreeVisible);

        panel.update();

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
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            tree.updateCellBackgroundValues(property, values);
        } else {
            panel.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            tree.updateCellForegroundValues(property, values);
        } else {
            panel.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            tree.updatePropertyCaptions(property, values);
        } else {
            panel.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (!property.grid) {
            panel.updateShowIfValues(property, values);
        }
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            tree.updateReadOnlyValues(property, values);
        } else {
            panel.updateReadOnlyValues(property, values);
        }
    }

    @Override
    public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        tree.updateRowBackgroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowBackgroundValue(values.firstValue());
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        tree.updateRowForegroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowForegroundValue(values.firstValue());
    }

    @Override
    public GGroupObjectValue getCurrentKey() {
        return tree.getSelectedKey();
    }

    @Override
    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet) {
        return tree.changeOrders(groupObject, value, alreadySet);
    }

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
    public GPropertyDraw getSelectedProperty() {
        return tree.getCurrentProperty();
    }

    @Override
    public GGroupObjectValue getSelectedColumnKey() {
        return null; // пока не поддерживаются группы в колонки
    }

    @Override
    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return tree.getSelectedValue(property);
    }

    @Override
    public List<Pair<Column, String>> getSelectedColumns() {
        return tree.getSelectedColumns(getSelectedGroupObject());
    }

    public boolean focusFirstWidget() {
        if (isShowing(tree)) {
            tree.focus();
            return true;
        }

        return panel.focusFirstWidget();
    }

    @Override
    public GComponent getGridComponent() {
        return treeGroup;
    }

    @Override
    protected void changeFilter(ArrayList<GPropertyFilter> conditions) {
        formController.changeFilter(treeGroup, conditions);
    }
    
    public boolean isExpandOnClick() {
        return treeGroup.expandOnClick;
    }

    public void fireExpandNodeRecursive(boolean current) {
        tree.fireExpandNodeRecursive(current);
    }

    public void fireCollapseNodeRecursive(boolean current) {
        tree.fireCollapseNodeRecursive(current);
    }
}
