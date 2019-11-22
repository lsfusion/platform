package lsfusion.client.form.object.table.grid.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.filter.user.controller.FilterController;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.panel.controller.PanelController;
import lsfusion.client.form.object.panel.controller.PropertyController;
import lsfusion.client.form.object.table.controller.AbstractTableController;
import lsfusion.client.form.object.table.grid.user.design.GridUserPreferences;
import lsfusion.client.form.object.table.grid.user.toolbar.view.CalculationsView;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.client.form.property.cell.classes.view.link.ImageLinkPropertyRenderer;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.order.user.Order;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static lsfusion.client.ClientResourceBundle.getString;

public class GridController extends AbstractTableController {
    private final ClientGroupObject groupObject;

    public GridTableController grid;

    protected CalculationsView calculationsView;

    public GridController(ClientGroupObject igroupObject, ClientFormController formController, final ClientFormLayout formLayout, GridUserPreferences[] userPreferences) throws IOException {
        super(formController, formLayout, igroupObject == null ? null : igroupObject.toolbar);
        groupObject = igroupObject;

        panel = new PanelController(GridController.this.formController, formLayout) {
            protected void addGroupObjectActions(final JComponent comp) {
                GridController.this.registerGroupObject(comp);
                if(filter != null) {
                    filter.getView().addActionsToPanelInputMap(comp);
                }
            }
        };

        if (groupObject != null) {
            calculationsView = new CalculationsView();
            formLayout.add(groupObject.calculations, calculationsView);
            
            if (groupObject.filter.visible) {
                filter = new FilterController(this, groupObject.filter) {
                    protected void remoteApplyQuery() {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    GridController.this.formController.changeFilter(groupObject, getConditions());
                                    grid.table.requestFocusInWindow();
                                } catch (IOException e) {
                                    throw new RuntimeException(ClientResourceBundle.getString("errors.error.applying.filter"), e);
                                }
                            }
                        });
                    }
                };

                filter.addView(formLayout);
            }

            // GRID идет как единый неделимый JComponent, поэтому смысла передавать туда FormLayout нет
            grid = new GridTableController(this, this.formController, userPreferences);
            registerGroupObject(grid.getGridView());
            if (filter != null) {
                filter.getView().addActionsToInputMap(grid.table);
            }
            grid.addView(formLayout);

            configureToolbar();
        }

        update();
    }

    private void configureToolbar() {
        if (filter != null) {
            addToToolbar(filter.getToolbarButton());
        }

        if (groupObject.toolbar.showGroupChange) {
            addToolbarSeparator();
            addToToolbar(grid.createGroupChangeButton());
        }

        if (groupObject.toolbar.showCountRows || groupObject.toolbar.showCalculateSum || groupObject.toolbar.showGroupReport) {
            addToolbarSeparator();
        }

        if (groupObject.toolbar.showCountRows) {
            addToToolbar(grid.createCountQuantityButton());
        }

        if (groupObject.toolbar.showCalculateSum) {
            addToToolbar(grid.createCalculateSumButton());
        }

        if (groupObject.toolbar.showGroupReport) {
            addToToolbar(grid.createGroupingButton());
        }

        if (groupObject.toolbar.showPrint || groupObject.toolbar.showXls) {
            addToolbarSeparator();
        }

        if (groupObject.toolbar.showPrint) {
            addToToolbar(grid.createPrintGroupButton());
        }

        if (groupObject.toolbar.showXls) {
            addToToolbar(grid.createPrintGroupXlsButton());
        }

        if (groupObject.toolbar.showSettings) {
            addToolbarSeparator();
            addToToolbar(grid.createGridSettingsButton());
        }
    }

    public void processFormChanges(ClientFormChanges fc,
                                   Map<ClientGroupObject, List<ClientGroupObjectValue>> cachedGridObjects
    ) {

        // Сначала меняем виды объектов
        for (ClientPropertyReader read : fc.properties.keySet()) // интересуют только свойства
        {
            if (read instanceof ClientPropertyDraw) {
                ClientPropertyDraw property = (ClientPropertyDraw) read;
                if (property.groupObject == groupObject && property.shouldBeDrawn(formController) && !fc.updateProperties.contains(property)) {
                    ImageLinkPropertyRenderer.clearChache(property);
                    
                    addDrawProperty(property);

                    OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>> groupColumnKeys = new OrderedMap<>();
                    for (ClientGroupObject columnGroupObject : property.columnGroupObjects) {
                        if (cachedGridObjects.containsKey(columnGroupObject)) {
                            groupColumnKeys.put(columnGroupObject, cachedGridObjects.get(columnGroupObject));
                        }
                    }

                    updateDrawColumnKeys(property, ClientGroupObject.mergeGroupValues(groupColumnKeys));
                }
            }
        }

        for (ClientPropertyDraw property : fc.dropProperties) {
            if (property.groupObject == groupObject) {
                dropProperty(property);
            }
        }

        if (isGrid()) {
            if (fc.gridObjects.containsKey(groupObject)) {
                setRowKeysAndCurrentObject(fc.gridObjects.get(groupObject), fc.objects.get(groupObject));
            }
        }

        // Затем их свойства
        for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
            ClientPropertyReader propertyRead = readProperty.getKey();
            if (propertyRead.getGroupObject() == groupObject && propertyRead.shouldBeDrawn(formController)) {
                propertyRead.update(readProperty.getValue(), fc.updateProperties.contains(propertyRead), this);
            }
        }

        update();
    }

    public void addDrawProperty(ClientPropertyDraw property) {
        if (property.grid) {
            grid.addProperty(property);
        } else {
            panel.addProperty(property);
        }
    }

    public void dropProperty(ClientPropertyDraw property) {
        if (grid != null) {
            grid.removeProperty(property);
        }

        panel.removeProperty(property);
    }

    public void setRowKeysAndCurrentObject(List<ClientGroupObjectValue> gridObjects, ClientGroupObjectValue newCurrentObject) {
        grid.setRowKeysAndCurrentObject(gridObjects, newCurrentObject);
    }

    public void modifyGroupObject(ClientGroupObjectValue gridObject, boolean add, int position) {
        assert isGrid();

        grid.modifyGridObject(gridObject, add, position); // assert что grid!=null

        grid.update();
    }

    public ClientGroupObjectValue getCurrentObject() {
        return grid != null && grid.getCurrentObject() != null ? grid.getCurrentObject() : ClientGroupObjectValue.EMPTY;
    }
    
    public int getCurrentRow() {
        return grid != null ? grid.table.getCurrentRow() : -1;
    }

    public void updateDrawColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> groupColumnKeys) {
        if (panel.containsProperty(property)) {
            panel.updateColumnKeys(property, groupColumnKeys);
        } else {
            grid.updateColumnKeys(property, groupColumnKeys);
        }
    }

    public void updateDrawPropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        if (panel.containsProperty(property)) {
            panel.updatePropertyCaptions(property, captions);
        } else {
            grid.updatePropertyCaptions(property, captions);
        }
    }

    public void updateShowIfs(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> showIfs) {
        if (panel.containsProperty(property)) {
            panel.updateShowIfs(property, showIfs);
        } else {
            grid.updateShowIfs(property, showIfs);
        }
    }

    @Override
    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (panel.containsProperty(property)) {
            panel.updateReadOnlyValues(property, values);
        } else {
            grid.updateReadOnlyValues(property, values);
        }
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        if (panel.containsProperty(property)) {
            panel.updateCellBackgroundValues(property, cellBackgroundValues);
        } else {
            grid.updateCellBackgroundValues(property, cellBackgroundValues);
        }
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        if (panel.containsProperty(property)) {
            panel.updateCellForegroundValues(property, cellForegroundValues);
        } else {
            grid.updateCellForegroundValues(property, cellForegroundValues);
        }
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean updateKeys) {
        if (panel.containsProperty(property)) {
            panel.updatePropertyValues(property, values, updateKeys);
        } else {
            grid.updatePropertyValues(property, values, updateKeys);
        }
    }

    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> rowBackground) {
        if (isGrid()) {
            grid.updateRowBackgroundValues(rowBackground);
        } else {
            panel.updateRowBackgroundValue((Color)BaseUtils.singleValue(rowBackground));
        }
    }

    public boolean isGrid() {
        return groupObject != null && groupObject.classView.isGrid();
    }
    
    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground) {
        if (isGrid()) {
            grid.updateRowForegroundValues(rowForeground);
        } else {
            panel.updateRowForegroundValue((Color)BaseUtils.singleValue(rowForeground));
        }
    }

    public void changeOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        if (grid != null) {
            grid.changeGridOrder(property, modiType);
        }
    }

    @Override
    public void clearOrders() throws IOException {
        if(grid != null) {
            grid.clearGridOrders(getGroupObject());
        }
    }

    public OrderedMap<ClientPropertyDraw, Boolean> getUserOrders() throws IOException {
        if (grid != null && grid.table.hasUserPreferences()) {
            OrderedMap<ClientPropertyDraw, Boolean> userOrders = new OrderedMap<>();
            List<ClientPropertyDraw> clientPropertyDrawList = getGroupObjectProperties();
            Collections.sort(clientPropertyDrawList, grid.table.getUserSortComparator());
            for (ClientPropertyDraw property : clientPropertyDrawList) {
                if (grid.table.getUserSort(property) != null && grid.table.getUserAscendingSort(property) != null) {
                    userOrders.put(property, grid.table.getUserAscendingSort(property));
                }
            }
            return userOrders;
        }
        return null;
    }

    public void applyUserOrders() throws IOException {
        OrderedMap<ClientPropertyDraw, Boolean> userOrders = getUserOrders();
        assert userOrders != null;
        formController.applyOrders(userOrders == null ? new OrderedMap<>() : userOrders, this);
    }
    
    public void applyDefaultOrders() throws IOException {
        formController.applyOrders(formController.getDefaultOrders(groupObject), this);
    }
    
    public GroupObjectUserPreferences getUserGridPreferences() {
        return grid.table.getCurrentUserGridPreferences();
    }

    public GroupObjectUserPreferences getGeneralGridPreferences() {
        return grid.table.getGeneralGridPreferences();
    }

    public void registerGroupObject(JComponent comp) {
        comp.putClientProperty("groupObject", groupObject);
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return formController.form.getPropertyDraws();
    }

    public ClientGroupObject getGroupObject() {
        return groupObject;
    }

    public ClientGroupObject getSelectedGroupObject() {
        return getGroupObject();
    }

    public List<ClientPropertyDraw> getGroupObjectProperties() {
        ArrayList<ClientPropertyDraw> properties = new ArrayList<>();
        for (ClientPropertyDraw property : getPropertyDraws()) {
            if (groupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }

        return properties;
    }
    
    public boolean isPropertyInGrid(ClientPropertyDraw property) {
        return grid != null && grid.containsProperty(property);
    }
    
    public boolean isPropertyInPanel(ClientPropertyDraw property) {
        return panel.containsProperty(property);
    }

    public ClientPropertyDraw getSelectedProperty() {
        return grid.getCurrentProperty();
    }
    public ClientGroupObjectValue getSelectedColumn() {
        return grid.getCurrentColumn();
    }

    public Object getSelectedValue(ClientPropertyDraw cell, ClientGroupObjectValue columnKey) {
        return grid.getSelectedValue(cell, columnKey);
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        if (filter != null) {
            filter.quickEditFilter(initFilterKeyEvent, propertyDraw, columnKey);
        }
    }

    public void selectProperty(ClientPropertyDraw propertyDraw) {
        grid.selectProperty(propertyDraw);
    }

    public void focusProperty(ClientPropertyDraw propertyDraw) {
        PropertyController propertyController = panel.getPropertyController(propertyDraw);
        if (propertyController != null) {
            propertyController.requestFocusInWindow();
        } else {
            grid.selectProperty(propertyDraw);
            grid.requestFocusInWindow();
        }
    }

    public void updateSelectionInfo(int quantity, String sum, String avg) {
        if (calculationsView != null) {
            calculationsView.updateSelectionInfo(quantity, sum, avg);
        }
    }

    private void update() {
        if (groupObject != null) {
            grid.update();

            if (toolbarView != null) {
                toolbarView.setVisible(grid.isVisible());
            }

            if (filter != null) {
                filter.setVisible(grid.isVisible());
            }
            
            if (calculationsView != null) {
                calculationsView.setVisible(grid.isVisible());
            }

            formController.setFiltersVisible(groupObject, grid.isVisible());
        }

        panel.update();
        panel.setVisible(true);
    }
}