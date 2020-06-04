package lsfusion.gwt.client.form.object.table.tree.controller;

import com.google.gwt.user.client.ui.Panel;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
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

import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;
import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;

public class GTreeGroupController extends GAbstractTableController {

    private final GTreeGroup treeGroup;

    private final Panel treeView;

    private final GTreeTable tree;
    
    private final GGroupObject lastGroupObject;

    private final GExpandTreeButton expandTreeButton;
    private final GExpandTreeButton expandTreeCurrentButton;

    public GTreeGroupController(GTreeGroup iTreeGroup, GFormController iFormController, GForm iForm) {
        super(iFormController, iTreeGroup.toolbar, true);
        treeGroup = iTreeGroup;
        lastGroupObject = treeGroup.groups.size() > 0 ? treeGroup.groups.get(treeGroup.groups.size() - 1) : null;

        tree = new GTreeTable(iFormController, iForm, this, treeGroup, treeGroup.autoSize);

        ResizableSimplePanel resizePanel = new ResizableSimplePanel();
        resizePanel.setStyleName("gridResizePanel");
        resizePanel.setFillWidget(tree);

        if(treeGroup.autoSize) { // убираем default'ый minHeight
            resizePanel.getElement().getStyle().setProperty("minHeight", "0px");
            resizePanel.getElement().getStyle().setProperty("minWidth", "0px");
        }

        treeView = new GridPanel(resizePanel, resizePanel);

        getFormLayout().addBaseComponent(treeGroup, treeView, new DefaultFocusReceiver() {
            @Override
            public boolean focus() {
                boolean focused = focusFirstWidget();
                if (focused) {
                    scrollToTop();
                }
                return focused;
            }
        });

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

            for (Map.Entry<GPropertyReader, HashMap<GGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
                if (readProperty.getKey() instanceof GPropertyDraw) {
                    GPropertyDraw property = (GPropertyDraw) readProperty.getKey();
                    if (property.groupObject == group)
                        updateProperty(group, property, GGroupObjectValue.SINGLE_EMPTY_KEY_LIST, fc.updateProperties.contains(property), readProperty.getValue());
                }
            }
            
            for (Map.Entry<GPropertyReader, HashMap<GGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
                if (!(readProperty.getKey() instanceof GPropertyDraw)) {
                    GPropertyReader propertyReader = readProperty.getKey();
                    if (formController.getGroupObject(propertyReader.getGroupObjectID()) == group) {
                        propertyReader.update(this, readProperty.getValue(), propertyReader instanceof GPropertyDraw && fc.updateProperties.contains(propertyReader));
                    }
                }
            }

            if (fc.objects.containsKey(group)) {
                tree.setCurrentPath(fc.objects.get(group));
            }
        }
        update();
    }

    public void afterAppliedChanges() {
        tree.afterAppliedChanges();
    }

    private void removeProperty(GGroupObject group, GPropertyDraw property) {
        if(property.grid)
            tree.removeProperty(group, property);
        else
            panel.removeProperty(property);
    }

    private void updateProperty(GGroupObject group, GPropertyDraw property, List<GGroupObjectValue> columnKeys, boolean updateKeys, HashMap<GGroupObjectValue, Object> values) {
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

        if (toolbarView != null) {
            toolbarView.setVisible(isTreeVisible);
        }

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

    public void beforeHidingGrid() {
        tree.beforeHiding();
    }

    public void afterShowingGrid() {
        tree.afterShowing();
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            tree.updateCellBackgroundValues(property, values);
        } else {
            panel.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            tree.updateCellForegroundValues(property, values);
        } else {
            panel.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            tree.updatePropertyCaptions(property, values);
        } else {
            panel.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (!property.grid) {
            panel.updateShowIfValues(property, values);
        }
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            tree.updateReadOnlyValues(property, values);
        } else {
            panel.updateReadOnlyValues(property, values);
        }
    }

    @Override
    public void updateLastValues(GLastReader reader, Map<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        tree.updateRowBackgroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowBackgroundValue(values.values().iterator().next());
    }

    @Override
    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {
        tree.updateRowForegroundValues(values);
        if (values != null && !values.isEmpty())
            panel.updateRowForegroundValue(values.values().iterator().next());
    }

    @Override
    public GGroupObjectValue getCurrentKey() {
        return tree.getCurrentKey();
    }

    @Override
    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet) {
        return tree.changeOrders(groupObject, value, alreadySet);
    }

    @Override
    public GGroupObject getSelectedGroupObject() {
        GTreeGridRecord record = tree.getSelectedRecord();
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
    public GGroupObjectValue getSelectedColumn() {
        return null; // пока не поддерживаются группы в колонки
    }

    @Override
    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return tree.getSelectedValue(property);
    }

    public boolean focusFirstWidget() {
        if (isShowing(tree)) {
            tree.setFocus(true);
            return true;
        }

        return panel.focusFirstWidget();
    }

    @Override
    public GComponent getGridComponent() {
        return treeGroup;
    }

    @Override
    protected void changeFilter(List<GPropertyFilter> conditions) {
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
