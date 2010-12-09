package platform.client.form;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.client.form.grid.GridController;
import platform.client.form.grid.GridView;
import platform.client.form.panel.PanelController;
import platform.client.form.showtype.ShowTypeController;
import platform.client.logics.*;
import platform.interop.ClassViewType;
import platform.interop.Order;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.*;

public class GroupObjectController implements GroupObjectLogicsSupplier {

    private final ClientGroupObject groupObject;
    private final LogicsSupplier logicsSupplier;
    private final ClientFormController form;

    private final PanelController panel;
    private GridController grid;
    private ShowTypeController showType;
    private final Map<ClientObject, ObjectController> objects = new HashMap<ClientObject, ObjectController>();

    private ClientGroupObjectValue currentObject;

    public ClassViewType classView = ClassViewType.HIDE;

    public GroupObjectController(ClientGroupObject igroupObject, LogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout formLayout) throws IOException {

        groupObject = igroupObject;
        logicsSupplier = ilogicsSupplier;
        form = iform;

        panel = new PanelController(this, form, formLayout) {

            protected void addGroupObjectActions(JComponent comp) {
                GroupObjectController.this.addGroupObjectActions(comp);
            }
        };

        if (groupObject != null) {

            // GRID идет как единый неделимый JComponent, поэтому смысла передавать туда FormLayout нет
            grid = new GridController(groupObject.grid, this, form);
            addGroupObjectActions(grid.getView());

            grid.addView(formLayout);

            for (ClientObject object : groupObject.objects) {

                objects.put(object, new ObjectController(object, form));
                objects.get(object).addView(formLayout);
            }

            showType = new ShowTypeController(groupObject.showType, this, form) {

                protected void needToBeShown() {
                    GroupObjectController.this.showViews();
                }

                protected void needToBeHidden() {
                    GroupObjectController.this.hideViews();
                }
            };

            showType.setBanClassView(groupObject.banClassView);

            showType.addView(formLayout);

            setClassView(ClassViewType.GRID);
            grid.update();
        }
    }

    private Set<ClientPropertyDraw> gridProperties = new HashSet<ClientPropertyDraw>();
    private Set<ClientPropertyDraw> panelProperties = new HashSet<ClientPropertyDraw>();

    public void processFormChanges(ClientFormChanges fc,
                                   Map<ClientGroupObject, List<ClientGroupObjectValue>> cachedGridObjects
    ) {

        // Сначала меняем виды объектов
        for (ClientPropertyRead read : fc.properties.keySet()) // интересуют только свойства
        {
            if (read instanceof ClientPropertyDraw) {
                ClientPropertyDraw property = (ClientPropertyDraw) read;
                if (property.groupObject == groupObject && property.shouldBeDrawn(form)) {
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

        // Затем подгружаем новые данные

        // Сначала новые объекты
        if (fc.gridObjects.containsKey(groupObject)) {
            setGridObjects(fc.gridObjects.get(groupObject));
        }

        if (fc.objects.containsKey(groupObject)) {
            setCurrentGroupObject(fc.objects.get(groupObject));
        }

        if (fc.classViews.containsKey(groupObject)) {
            setClassView(fc.classViews.get(groupObject));
            requestFocusInWindow();
        }

        // Затем их свойства
        for (Map.Entry<ClientPropertyRead, Map<ClientGroupObjectValue, Object>> readProperty : fc.properties.entrySet()) {
            ClientPropertyRead propertyRead = readProperty.getKey();
            if (propertyRead.getGroupObject() == groupObject && propertyRead.shouldBeDrawn(form)) {
                propertyRead.update(readProperty.getValue(), this);
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
            gridProperties.remove(property);
            grid.removeProperty(property);
        }

        panel.addProperty(property);
        panelProperties.add(property);
    }

    public void addGridProperty(ClientPropertyDraw property) {
        gridProperties.add(property);
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
            gridProperties.remove(property);
        }

        panel.removeProperty(property);
        panelProperties.remove(property);
    }

    public ClientGroupObjectValue getCurrentObject() {
        return currentObject;
    }

    public void setGridObjects(List<ClientGroupObjectValue> gridObjects) {
        grid.setGridObjects(gridObjects);

        if (grid.getKey().autoHide) {
            setClassView(gridObjects.size() != 0 ? ClassViewType.GRID : ClassViewType.HIDE);
            grid.update();
        }
    }

    public void setCurrentGroupObject(ClientGroupObjectValue value) {
        if (!value.equals(currentObject)) {
            currentObject = value;
            grid.selectObject(currentObject);
        }
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

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        if (panelProperties.contains(property)) {
            panel.updatePropertyValues(property, values);
        } else {
            grid.updatePropertyValues(property, values);
        }
    }

    public void updateDrawHighlightValues(Map<ClientGroupObjectValue, Object> highlights) {
        if (classView == ClassViewType.GRID) {
            grid.updateHighlightValues(highlights);
        } else {
            panel.updateHighlightValue(BaseUtils.singleValue(highlights));
        }
    }


    public void changeGridOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        if (grid != null) {
            grid.changeGridOrder(property, modiType);
        }
    }

    // приходится делать именно так, так как логика отображения одного GroupObject може не совпадать с логикой Container-Component
    void addGroupObjectActions(JComponent comp) {

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK), "switchClassView");
        comp.getActionMap().put("switchClassView", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    form.switchClassView(groupObject);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при изменении вида", e);
                }
            }
        });

        // вот так вот приходится делать, чтобы "узнавать" к какому GroupObject относится этот Component
        comp.putClientProperty("groupObject", groupObject);
    }

    // реализация GroupObjectLogicsSupplier
    public List<ClientObject> getObjects() {
        return logicsSupplier.getObjects();
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return logicsSupplier.getPropertyDraws();
    }

    public ClientGroupObject getGroupObject() {
        return groupObject;
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

    public ClientPropertyDraw getDefaultProperty() {
        return grid.getCurrentProperty();
    }

    public Object getSelectedValue(ClientPropertyDraw cell) {
        return grid.getSelectedValue(cell);
    }

    public ClientFormController getForm() {
        return form;
    }

    public String getSaveMessage() {

        String message = "";
        for (ClientObject object : groupObject.objects) {
            if (object.addOnTransaction) {
                message += "Создать новый " + object.getCaption() + " ?";
            }
        }

        return message;
    }

    public GridView getGridView() {
        return grid.getView();
    }
}