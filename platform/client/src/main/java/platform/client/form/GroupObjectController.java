package platform.client.form;

import platform.client.logics.*;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.form.panel.PanelController;
import platform.client.form.grid.GridController;
import platform.interop.Order;

import javax.swing.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

public class GroupObjectController implements GroupObjectLogicsSupplier {

    private final ClientGroupObjectImplementView groupObject;
    private final LogicsSupplier logicsSupplier;
    private final ClientForm form;

    private final PanelController panel;
    private GridController grid;
    public final Map<ClientObjectImplementView, ObjectController> objects = new HashMap<ClientObjectImplementView, ObjectController>();

    ClientGroupObjectValue currentObject;

    Boolean classView;

    public GroupObjectController(ClientGroupObjectImplementView igroupObject, LogicsSupplier ilogicsSupplier, ClientForm iform, ClientFormLayout formLayout) throws IOException {

        groupObject = igroupObject;
        logicsSupplier = ilogicsSupplier; 
        form = iform;

        if (groupObject != null) {

            // Grid идет как единый неделимый JComponent, поэтому смысла передавать туда FormLayout нет
            grid = new GridController(groupObject.gridView, this, form);
            if (!form.isReadOnly()) addGroupObjectActions(grid.getView());

            grid.addView(formLayout);

            for (ClientObjectImplementView object : groupObject) {

                objects.put(object, new ObjectController(object, form));
                objects.get(object).addView(formLayout);
            }
        }

        panel = new PanelController(this, form, formLayout) {
            protected void addGroupObjectActions(JComponent comp) {
                GroupObjectController.this.addGroupObjectActions(comp);
            }
        };

    }

    public void setClassView(Boolean setClassView) {

        if (classView == null || classView != setClassView) {

            classView = setClassView;
            if (classView) {
                panel.removeGroupObjectID();
                grid.addGroupObjectID();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        grid.requestFocusInWindow();
                    }
                });
            } else {
                panel.addGroupObjectID();
                grid.removeGroupObjectID();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        panel.requestFocusInWindow();
                    }
                });
//                    panel.requestFocusInWindow();
            }

            for (ClientObjectImplementView object : groupObject) {
                objects.get(object).changeClassView(classView);
            }

        }

    }

    public void addPanelProperty(ClientPropertyView property) {

        if (grid != null)
            grid.removeProperty(property);
        
        panel.addProperty(property);

    }

    public void addGridProperty(ClientPropertyView property) {

        panel.removeProperty(property);
        grid.addProperty(property);

    }

    public void dropProperty(ClientPropertyView property) {

        panel.removeProperty(property);
        grid.removeProperty(property);

    }

    public ClientGroupObjectValue getCurrentObject() {
        return currentObject;
    }

    public void setGridObjects(List<ClientGroupObjectValue> gridObjects) {
        grid.setGridObjects(gridObjects);
    }

    public void setCurrentGroupObject(ClientGroupObjectValue value, Boolean userChange) {

        boolean realChange = !value.equals(currentObject);

/*            if (currentObject != null)
            System.out.println("view - oldval : " + currentObject.toString() + " ; userChange " + userChange.toString() );
        if (value != null)
            System.out.println("view - newval : " + value.toString() + " ; userChange " + userChange.toString());*/

        currentObject = value;

        if (realChange) {

            panel.selectObject(currentObject);
            if (!userChange) // если не сам изменил, а то пойдет по кругу
                grid.selectObject(currentObject);
        }

    }

    public void setCurrentObject(ClientObjectImplementView object, Integer value) {

        if (currentObject == null) return;

        ClientGroupObjectValue curValue = (ClientGroupObjectValue) currentObject.clone();

        curValue.put(object, value);
        setCurrentGroupObject(curValue, false);
    }

    public void setPanelPropertyValue(ClientPropertyView property, Object value) {

        panel.setPropertyValue(property, value);
    }

    public void setGridPropertyValues(ClientPropertyView property, Map<ClientGroupObjectValue,Object> values) {

        grid.setPropertyValues(property, values);
    }

    public void changeGridOrder(ClientCellView property, Order modiType) throws IOException {
        grid.changeGridOrder(property, modiType);
    }
    
    // приходится делать именно так, так как логика отображения одного GroupObject може не совпадать с логикой Container-Component
    public void addGroupObjectActions(JComponent comp) {

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "switchClassView");
        comp.getActionMap().put("switchClassView", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    form.switchClassView(groupObject);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при изменении вида", e);
                }
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_DOWN_MASK), "addObject");
        comp.getActionMap().put("addObject", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                ClientObjectClass addClass = objects.get(groupObject.get(0)).classController.getDerivedClass();
                if(addClass instanceof ClientConcreteClass) {
                    try {
                        form.addObject(groupObject.get(0),(ClientConcreteClass)addClass);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при добавлении объекта", e);
                    }
                }
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_DOWN_MASK), "removeObject");
        comp.getActionMap().put("removeObject", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    form.changeClass(groupObject.get(0), null);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при удалении объекта", e);
                }
            }
        });

        comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_DOWN_MASK), "changeObjectClass");
        comp.getActionMap().put("changeObjectClass", new AbstractAction() {

            public void actionPerformed(ActionEvent ae) {
                ClientObjectClass changeClass = objects.get(groupObject.get(0)).classController.getSelectedClass();
                if(changeClass instanceof ClientConcreteClass) {
                    try {
                        form.changeClass(groupObject.get(0), (ClientConcreteClass) changeClass);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при изменении класса объекта", e);
                    }
                }
            }
        });

    }

    // реализация GroupObjectLogicsSupplier

    public List<ClientObjectImplementView> getObjects() {
        return logicsSupplier.getObjects();
    }

    public List<ClientPropertyView> getProperties() {
        return logicsSupplier.getProperties();
    }

    public List<ClientCellView> getCells() {
        return logicsSupplier.getCells();
    }

    public ClientGroupObjectImplementView getGroupObject() {
        return groupObject;
    }

    public List<ClientPropertyView> getGroupObjectProperties() {

        ArrayList<ClientPropertyView> properties = new ArrayList<ClientPropertyView>();
        for (ClientPropertyView property : getProperties()) {
            if (groupObject.equals(property.groupObject))
                properties.add(property);
        }

        return properties;
    }

    public ClientPropertyView getDefaultProperty() {

        ClientCellView currentCell = grid.getCurrentCell();
        if (currentCell instanceof ClientPropertyView)
            return (ClientPropertyView) currentCell;
        else
            return null;
    }

    public Object getSelectedValue(ClientPropertyView cell) {
        return grid.getSelectedValue(cell);
    }

    public ClientForm getForm() {
        return form;
    }
}