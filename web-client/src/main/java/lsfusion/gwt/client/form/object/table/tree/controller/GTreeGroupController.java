package lsfusion.gwt.client.form.object.table.tree.controller;

import com.google.gwt.user.client.ui.Panel;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.view.GFormLayoutImpl;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.grid.user.design.view.GExpandTreeButton;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeGridRecord;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;
import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;

public class GTreeGroupController extends GAbstractTableController {
    private static final GFormLayoutImpl layoutImpl = GFormLayoutImpl.get();
    
    private final GTreeGroup treeGroup;

    private final Panel treeView;

    private final GTreeTable tree;
    
    private final GGroupObject lastGroupObject;

    private final GExpandTreeButton expandTreeButton;
    private final GExpandTreeButton expandTreeCurrentButton;

    public GTreeGroupController(GTreeGroup iTreeGroup, GFormController iFormController, GForm iForm) {
        super(iFormController, iTreeGroup.toolbar);
        treeGroup = iTreeGroup;
        lastGroupObject = treeGroup.groups.size() > 0 ? treeGroup.groups.get(treeGroup.groups.size() - 1) : null;

        tree = new GTreeTable(iFormController, iForm, this, treeGroup, treeGroup.autoSize);

        ResizableSimplePanel resizePanel = new ResizableSimplePanel();
        resizePanel.setStyleName("gridResizePanel");
        resizePanel.setWidget(tree);
        setupFillParent(resizePanel.getElement(), tree.getElement());

        if(treeGroup.autoSize) { // убираем default'ый minHeight
            resizePanel.getElement().getStyle().setProperty("minHeight", "0px");
            resizePanel.getElement().getStyle().setProperty("minWidth", "0px");
        }

        treeView = layoutImpl.createTreeView(treeGroup, resizePanel);

        getFormLayout().add(treeGroup, treeView, new DefaultFocusReceiver() {
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

            for (GPropertyReader propertyReader : fc.properties.keySet()) {
                if (propertyReader instanceof GPropertyDraw) {
                    GPropertyDraw property = (GPropertyDraw) propertyReader;
                    if (property.groupObject == group && !fc.updateProperties.contains(property)) {
                        addProperty(group, property, fc.panelProperties.contains(property));

                        //пока не поддерживаем группы в колонках в дереве, поэтому делаем
                        if (panel.containsProperty(property)) {
                            panel.updateColumnKeys(property, GGroupObjectValue.SINGLE_EMPTY_KEY_LIST);
                        }
                    }
                }
            }

            for (GPropertyDraw property : fc.dropProperties) {
                if (property.groupObject == group) {
                    removeProperty(group, property);
                }
            }

            for (Map.Entry<GPropertyReader, HashMap<GGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
                GPropertyReader propertyReader = readProperty.getKey();
                if (formController.getGroupObject(propertyReader.getGroupObjectID()) == group) {
                    propertyReader.update(this, readProperty.getValue(), propertyReader instanceof GPropertyDraw && fc.updateProperties.contains(propertyReader));
                }
            }

            if (fc.objects.containsKey(group)) {
                tree.setCurrentPath(fc.objects.get(group));
            }
        }
        update();
    }

    public void restoreScrollPosition() {
        tree.restoreScrollPosition();
    }

    private void removeProperty(GGroupObject group, GPropertyDraw property) {
        panel.removeProperty(property);
        tree.removeProperty(group, property);
    }

    private void addProperty(GGroupObject group, GPropertyDraw property, boolean toPanel) {
        if (toPanel) {
            addPanelProperty(group, property);
        } else {
            addGridProperty(group, property);
        }
    }

    private void addPanelProperty(GGroupObject group, GPropertyDraw property) {
        tree.removeProperty(group, property);
        panel.addProperty(property);
    }

    private void addGridProperty(GGroupObject group, GPropertyDraw property) {
        tree.addProperty(group, property);
        panel.removeProperty(property);
    }

    private void update() {
        tree.update();

        tree.restoreVisualState();

        boolean isTreeVisible = tree.getColumnCount() > 1;

        treeView.setVisible(isTreeVisible);

        if (toolbarView != null) {
            toolbarView.setVisible(isTreeVisible);
        }

        if (filter != null) {
            filter.setVisible(isTreeVisible);
        }

        for (GGroupObject groupObject : treeGroup.groups) {
            formController.setFiltersVisible(groupObject, isTreeVisible);
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
    public void updatePropertyDrawValues(GPropertyDraw reader, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        GPropertyDraw property = formController.getProperty(reader.ID);
        if (panel.containsProperty(property)) {
            panel.updatePropertyValues(property, values, updateKeys);
        } else {
            tree.updatePropertyValues(property, values, updateKeys);
        }
    }

    @Override
    public void updateBackgroundValues(GBackgroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellBackgroundValues(property, values);
        } else {
            tree.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateForegroundValues(GForegroundReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateCellForegroundValues(property, values);
        } else {
            tree.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updateCaptionValues(GCaptionReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updatePropertyCaptions(property, values);
        } else {
            tree.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateShowIfValues(property, values);
        }
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, Map<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (panel.containsProperty(property)) {
            panel.updateReadOnlyValues(property, values);
        } else {
            tree.updateReadOnlyValues(property, values);
        }
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
    public void changeOrder(GPropertyDraw property, GOrder modiType) {
        tree.changeOrder(property, modiType);
    }

    @Override
    public void clearOrders(GGroupObject groupObject) {
        tree.clearOrders(groupObject);
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
