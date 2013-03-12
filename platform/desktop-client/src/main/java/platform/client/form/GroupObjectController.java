package platform.client.form;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.client.ClientResourceBundle;
import platform.client.Main;
import platform.client.form.grid.GridController;
import platform.client.form.panel.PanelController;
import platform.client.form.queries.FilterController;
import platform.client.form.showtype.ShowTypeController;
import platform.client.logics.*;
import platform.interop.ClassViewType;
import platform.interop.KeyStrokes;
import platform.interop.Order;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static platform.client.ClientResourceBundle.getString;

public class GroupObjectController extends AbstractGroupObjectController {
    private final ClientGroupObject groupObject;

    public GridController grid;
    public ShowTypeController showType;
    public FilterController filter;

    private final Map<ClientObject, ObjectController> objects = new HashMap<ClientObject, ObjectController>();

    public ClassViewType classView = ClassViewType.GRID;

    public GroupObjectController(ClientGroupObject igroupObject, LogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout formLayout) throws IOException {
        super(iform, ilogicsSupplier, formLayout, igroupObject == null ? null : igroupObject.toolbar);
        groupObject = igroupObject;

        panel = new PanelController(this, form, formLayout) {
            protected void addGroupObjectActions(JComponent comp) {
                GroupObjectController.this.addGroupObjectActions(comp);
            }
        };

        if (groupObject != null) {
            if (groupObject.filter.visible) {
                filter = new FilterController(this, groupObject.filter) {
                    protected void remoteApplyQuery() {
                        try {
                            form.changeFilter(groupObject, getConditions());
                        } catch (IOException e) {
                            throw new RuntimeException(ClientResourceBundle.getString("errors.error.applying.filter"), e);
                        }

                        grid.table.requestFocusInWindow();
                    }
                };

                filter.addView(formLayout);
            }

            for (ClientObject object : groupObject.objects) {
                objects.put(object, new ObjectController(object, form));
                objects.get(object).addView(formLayout);
            }

            // GRID идет как единый неделимый JComponent, поэтому смысла передавать туда FormLayout нет
            grid = new GridController(this, form);
            addGroupObjectActions(grid.getGridView());
            if (filter != null) {
                filter.getView().addActionsToInputMap(grid.table);
            }
            grid.addView(formLayout);

            showType = new ShowTypeController(groupObject, this, form);
            showType.addView(formLayout);

            configureToolbar();
        }

        update();
    }

    private void configureToolbar() {
        boolean hasClassChoosers = false;
        for (final ClientObject object : groupObject.grid.groupObject.objects) {
            if (object.classChooser.visible) {
                addToToolbar(getObjectController(object).getToolbarButton());
                hasClassChoosers = true;
            }
        }
        if (hasClassChoosers) {
            addToToolbar(Box.createHorizontalStrut(5));
        }

        if (filter != null) {
            addToToolbar(filter.getToolbarButton());
            addToToolbar(Box.createHorizontalStrut(5));
        }

        if (groupObject.toolbar.showGroupChange) {
            addToToolbar(grid.createGroupChangeButton());
        }

        if (groupObject.toolbar.showCountRows) {
            addToToolbar(grid.createCountQuantityButton());
        }

        if (groupObject.toolbar.showCalculateSum) {
            addToToolbar(grid.craeteCalculateSumButton());
        }

        if (groupObject.toolbar.showGroupReport) {
            addToToolbar(grid.createGroupButton());
        }

        addToToolbar(Box.createHorizontalStrut(5));

        if (groupObject.toolbar.showPrint && Main.module.isFull()) { // todo [dale]: Можно ли избавиться от if'ов?
            addToToolbar(grid.createPrintGroupButton());
        }

        if (groupObject.toolbar.showXls && Main.module.isFull()) {
            addToToolbar(grid.createPrintGroupXlsButton());
            addToToolbar(Box.createHorizontalStrut(5));
        }

        if (groupObject.toolbar.showSettings) {
            addToToolbar(grid.createHideSettingsButton());
            addToToolbar(Box.createHorizontalStrut(5));
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
                if (property.groupObject == groupObject && property.shouldBeDrawn(form) && !fc.updateProperties.contains(property)) {
                    addDrawProperty(property, fc.panelProperties.contains(property));

                    OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>> groupColumnKeys = new OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>>();
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

        ClassViewType newClassView = fc.classViews.get(groupObject);
        if (newClassView != null && classView != newClassView) {
            setClassView(newClassView);
            requestFocusInWindow();
        }

        // Затем подгружаем новые данные

        if (classView == ClassViewType.GRID) {
            // Сначала новые объекты
            if (fc.gridObjects.containsKey(groupObject)) {
                setGridObjects(fc.gridObjects.get(groupObject));
            }

            if (fc.objects.containsKey(groupObject)) {
                setCurrentObject(fc.objects.get(groupObject));
            }
        }

        // Затем их свойства
        for (Map.Entry<ClientPropertyReader, Map<ClientGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
            ClientPropertyReader propertyRead = readProperty.getKey();
            if (propertyRead.getGroupObject() == groupObject && propertyRead.shouldBeDrawn(form)) {
                propertyRead.update(readProperty.getValue(), fc.updateProperties.contains(propertyRead), this);
            }
        }

        update();
    }

    public void setClassView(ClassViewType classView) {
        this.classView = classView;
    }

    public void requestFocusInWindow() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (classView == ClassViewType.GRID) {
                    grid.requestFocusInWindow();
                } else if (classView == ClassViewType.PANEL) {
                    panel.requestFocusInWindow();
                }
            }
        });
    }

    public void addPanelProperty(ClientPropertyDraw property) {
        if (grid != null) {
            grid.removeProperty(property);
        }

        panel.addProperty(property);
    }

    public void addGridProperty(ClientPropertyDraw property) {
        grid.addProperty(property);

        panel.removeProperty(property);
    }

    public void addDrawProperty(ClientPropertyDraw property, boolean toPanel) {
        if (toPanel) {
            addPanelProperty(property);
        } else {
            addGridProperty(property);
        }
    }

    public void dropProperty(ClientPropertyDraw property) {
        if (grid != null) {
            grid.removeProperty(property);
        }

        panel.removeProperty(property);
    }

    public void setGridObjects(List<ClientGroupObjectValue> gridObjects) {
        grid.setGridObjects(gridObjects);

        if (groupObject.grid.autoHide) {
            setClassView(gridObjects.size() != 0 ? ClassViewType.GRID : ClassViewType.HIDE);
            update();
        }
    }

    public void modifyGroupObject(ClientGroupObjectValue gridObject, boolean add) {
        assert classView == ClassViewType.GRID;

        grid.modifyGridObject(gridObject, add); // assert что grid!=null

        assert !groupObject.grid.autoHide;
        grid.update();
    }

    public void setCurrentObject(ClientGroupObjectValue value) {
        grid.setCurrentObject(value);
    }

    public ClientGroupObjectValue getCurrentObject() {
        return grid != null && grid.getCurrentObject() != null ? grid.getCurrentObject() : ClientGroupObjectValue.EMPTY;
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

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        if (panel.containsProperty(property)) {
            panel.updateCellBackgroundValue(property, cellBackgroundValues);
        } else {
            grid.updateCellBackgroundValues(property, cellBackgroundValues);
        }
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        if (panel.containsProperty(property)) {
            panel.updateCellForegroundValue(property, cellForegroundValues);
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
        if (classView == ClassViewType.GRID) {
            grid.updateRowBackgroundValues(rowBackground);
        } else {
            panel.updateRowBackgroundValue((Color)BaseUtils.singleValue(rowBackground));
        }
    }

    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground) {
        if (classView == ClassViewType.GRID) {
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

    // приходится делать именно так, так как логика отображения одного GroupObject може не совпадать с логикой Container-Component
    void addGroupObjectActions(JComponent comp) {

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getSwitchClassViewKeyStroke(), "switchClassView");
        comp.getActionMap().put("switchClassView", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    form.switchClassView(groupObject);
                } catch (IOException e) {
                    throw new RuntimeException(getString("errors.error.changing.type"), e);
                }
            }
        });

        // вот так вот приходится делать, чтобы "узнавать" к какому GroupObject относится этот Component
        comp.putClientProperty("groupObject", groupObject);
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return logicsSupplier.getPropertyDraws();
    }

    public ClientGroupObject getGroupObject() {
        return groupObject;
    }

    public ClientGroupObject getSelectedGroupObject() {
        return getGroupObject();
    }

    public ObjectController getObjectController(ClientObject object) {
        return objects.get(object);
    }

    public List<ClientPropertyDraw> getGroupObjectProperties() {
        ArrayList<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();
        for (ClientPropertyDraw property : getPropertyDraws()) {
            if (groupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }

        return properties;
    }

    public ClientPropertyDraw getSelectedProperty() {
        ClientPropertyDraw defaultProperty = groupObject.filterProperty;
        return defaultProperty != null
                ? defaultProperty
                : grid.getCurrentProperty();
    }

    public Object getSelectedValue(ClientPropertyDraw cell, ClientGroupObjectValue columnKey) {
        return grid.getSelectedValue(cell, columnKey);
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent) {
        quickEditFilter(initFilterKeyEvent, null);
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw) {
        if (filter != null) {
            filter.quickEditFilter(initFilterKeyEvent, propertyDraw);
            grid.selectProperty(propertyDraw);
        }
    }

    public void selectProperty(ClientPropertyDraw propertyDraw) {
        grid.selectProperty(propertyDraw);
    }

    public void updateSelectionInfo(int quantity, String sum, String avg) {
        toolbarView.updateSelectionInfo(quantity, sum, avg);
    }

    private void update() {
        if (groupObject != null) {
            grid.update();

            toolbarView.setVisible(grid.isVisible());

            if (filter != null) {
                filter.setVisible(grid.isVisible());
            }

            for (ClientObject object : groupObject.objects) {
                objects.get(object).setVisible(grid.isVisible());
            }

            showType.update(classView);
        }

        panel.update();
        panel.setVisible(classView != ClassViewType.HIDE);
    }
}