package platform.client.form;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.client.form.cell.PropertyController;
import platform.client.form.grid.GridController;
import platform.client.form.panel.PanelController;
import platform.client.form.panel.PanelShortcut;
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

    private final Map<ClientObject, ObjectController> objects = new HashMap<ClientObject, ObjectController>();

    public ClassViewType classView = ClassViewType.HIDE;

    public GroupObjectController(ClientGroupObject igroupObject, LogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout formLayout) throws IOException {
        super(iform, ilogicsSupplier, formLayout);
        groupObject = igroupObject;

        panel = new PanelController(this, form, formLayout) {
            protected void addGroupObjectActions(JComponent comp) {
                GroupObjectController.this.addGroupObjectActions(comp);
            }
        };

        panelShortcut = new PanelShortcut(form, panel);

        if (groupObject != null) {

            // GRID идет как единый неделимый JComponent, поэтому смысла передавать туда FormLayout нет
            grid = new GridController(this, form);
            addGroupObjectActions(grid.getGridView());

            for (ClientObject object : groupObject.objects) {

                objects.put(object, new ObjectController(object, form));
                objects.get(object).addView(formLayout);
            }

            grid.addView(formLayout);

            showType = new ShowTypeController(groupObject.showType, this, form) {

                protected void needToBeShown() {
                    GroupObjectController.this.showViews();
                }

                protected void needToBeHidden() {
                    GroupObjectController.this.hideViews();
                }
            };

            showType.setBanClassView(groupObject.banClassView);

            setClassView(ClassViewType.GRID);
            grid.update();
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
                if (!fc.updateProperties.contains(property) && property.groupObject == groupObject && property.shouldBeDrawn(form)) {
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

        // Сначала новые объекты
        if (fc.gridObjects.containsKey(groupObject)) {
            setGridObjects(fc.gridObjects.get(groupObject));
        }

        if (fc.objects.containsKey(groupObject)) {
            setCurrentObject(fc.objects.get(groupObject));
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

    private void update() {
        if (grid != null) {
            grid.update();
        }
        panel.update();
    }

    private void hideViews() {

        panel.hideViews();

        if (grid != null) {
            grid.hideViews();
        }

        if (groupObject != null) {
            for (ClientObject object : groupObject.objects) {
                objects.get(object).hideViews();
            }
        }

        if (showType != null) {
            showType.hideViews();
        }

        // нет смысла вызывать validate или invalidate, так как setVisible услышит сам SimplexLayout и сделает главному контейнеру invalidate
    }

    private void showViews() {
        panel.showViews();

        if (grid != null) {
            grid.showViews();
        }

        if (groupObject != null) {
            for (ClientObject object : groupObject.objects) {
                objects.get(object).showViews();
            }
        }

        if (showType != null) {
            showType.showViews();
        }
    }

    public void setClassView(ClassViewType classView) {
        if (this.classView != classView) {
            this.classView = classView;

            for (ClientObject object : groupObject.objects) {
                objects.get(object).changeClassView(classView);
            }

            if (showType != null) {
                showType.changeClassView(classView);
            }
        }
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
        panelProperties.add(property);
    }

    public void addGridProperty(ClientPropertyDraw property) {
        grid.addProperty(property);

        panel.removeProperty(property);
        panelProperties.remove(property);
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
        panelProperties.remove(property);
    }

    public void setGridObjects(List<ClientGroupObjectValue> gridObjects) {
        grid.setGridObjects(gridObjects);

        if (groupObject.grid.autoHide) {
            setClassView(gridObjects.size() != 0 ? ClassViewType.GRID : ClassViewType.HIDE);
            grid.update();
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
        if (panelProperties.contains(property)) {
            panel.updateColumnKeys(property, groupColumnKeys);
        } else {
            grid.updateColumnKeys(property, groupColumnKeys);
        }
    }

    public void updateDrawPropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        if (panelProperties.contains(property)) {
            panel.updatePropertyCaptions(property, captions);
        } else {
            grid.updatePropertyCaptions(property, captions);
        }
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        if (panelProperties.contains(property)) {
            panel.updateCellBackgroundValue(property, cellBackgroundValues);
        } else {
            grid.updateCellBackgroundValues(property, cellBackgroundValues);
        }
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        if (panelProperties.contains(property)) {
            panel.updateCellForegroundValue(property, cellForegroundValues);
        } else {
            grid.updateCellForegroundValues(property, cellForegroundValues);
        }
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update) {
        if (panelProperties.contains(property)) {
            panel.updatePropertyValues(property, values, update);
        } else {
            grid.updatePropertyValues(property, values, update);
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

    public Map<ClientObject, ObjectController> getObjectsMap() {
        return objects;
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
        grid.quickEditFilter(initFilterKeyEvent, propertyDraw);
    }

    public void selectProperty(ClientPropertyDraw propertyDraw) {
        grid.selectProperty(propertyDraw);
    }

    public void moveComponent(Component component, int destination) {
        panelToolbar.moveComponent(component, destination);
    }

    public void updateSelectionInfo(int quantity, String sum, String avg) {
        panelToolbar.updateSelectionInfo(quantity, sum, avg);
    }

    public JPanel getToolbarView() {
        return panelToolbar.getView();
    }

    public void updateToolbar() {
        if (groupObject != null) {
            if (classView == ClassViewType.GRID) {
                panelToolbar.removeComponent(showType.view);
                panelToolbar.update(classView);
                panelToolbar.addComponent(Box.createHorizontalStrut(5), true);
                panelToolbar.addComponent(showType.view, true);
            } else {
                for (Map.Entry<ClientRegularFilterGroup, JComponent> entry : panelToolbar.getFilters()) {
                    formLayout.add(entry.getKey(), entry.getValue());
                }

                for (PropertyController control : panelToolbar.getProperties()) {
                    control.addView(formLayout);
                    control.getPanelView().changeViewType(classView);
                }

                formLayout.add(showType.clientShowType, showType.view);
            }
        }
    }

    public void addFilterToToolbar(ClientRegularFilterGroup filterGroup, JComponent component) {
        panelToolbar.addFilter(filterGroup, component);
    }
}