package platform.client.form;

import platform.client.form.grid.GridController;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupObjectController implements GroupObjectLogicsSupplier {

    private final ClientGroupObject groupObject;
    private final LogicsSupplier logicsSupplier;
    private final ClientFormController form;

    private final PanelController panel;
    private GridController grid;
    private ShowTypeController showType;
    private final Map<ClientObject, ObjectController> objects = new HashMap<ClientObject, ObjectController>();

    private ClientGroupObjectValue currentObject;

    private byte classView = ClassViewType.HIDE;

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

            // Grid идет как единый неделимый JComponent, поэтому смысла передавать туда FormLayout нет
            grid = new GridController(groupObject.grid, this, form);
            addGroupObjectActions(grid.getView());

            grid.addView(formLayout);

            for (ClientObject object : groupObject) {

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
        }

    }

    private void hideViews() {

        panel.hideViews();

        if (grid != null)
            grid.hideViews();

        if (groupObject != null)
            for (ClientObject object : groupObject)
                objects.get(object).hideViews();

        if (showType != null)
            showType.hideViews();

        // нет смысла вызывать validate или invalidate, так как setVisible услышит сам SimplexLayout и сделает главному контейнеру invalidate
    }

    private void showViews() {

        panel.showViews();

        if (grid != null)
            grid.showViews();

        if (groupObject != null)
            for (ClientObject object : groupObject)
                objects.get(object).showViews();

        if (showType != null)
            showType.showViews();

    }

    public void setClassView(byte classView) {

        if (this.classView != classView) {
            this.classView = classView;
            if (classView == ClassViewType.GRID) {
                panel.removeGroupObjectCells();
                grid.addGroupObjectCells();
            } else if (classView == ClassViewType.PANEL) {
                panel.addGroupObjectCells();
                grid.removeGroupObjectCells();
            } else {
                panel.removeGroupObjectCells();
                grid.removeGroupObjectCells();
            }

            for (ClientObject object : groupObject) {
                objects.get(object).changeClassView(classView);
            }

            if (showType != null)
                showType.changeClassView(classView);
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

    public void addPanelProperty(ClientPropertyDraw property, Object value) {

        if (grid != null)
            grid.removeProperty(property);

        panel.addProperty(property, value);

    }

    public void addGridProperty(ClientPropertyDraw property) {

        panel.removeProperty(property);
        grid.addProperty(property);

    }

    public void dropProperty(ClientPropertyDraw property) {

        panel.removeProperty(property);
        grid.removeProperty(property);

    }

    public ClientGroupObjectValue getCurrentObject() {
        return currentObject;
    }

    public void setGridObjects(List<ClientGroupObjectValue> gridObjects) {
        grid.setGridObjects(gridObjects);

        if (grid.getGrid().autoHide) {
            setClassView(gridObjects.size() != 0 ? ClassViewType.GRID : ClassViewType.HIDE);
        }
    }

    public void setGridClasses(List<ClientGroupObjectClass> gridClasses) {
        grid.setGridClasses(gridClasses);
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

    public void setCurrentObject(ClientObject object, Object value) {

        if (currentObject == null) return;

        ClientGroupObjectValue curValue = (ClientGroupObjectValue) currentObject.clone();

        curValue.put(object, value);
        setCurrentGroupObject(curValue, false);
    }

    public void setCurrentGroupObjectClass(ClientGroupObjectClass value) {
        panel.setCurrentClass(value);
    }

    public void setPanelPropertyValue(ClientPropertyDraw property, Object value) {

        panel.setPropertyValue(property, value);
    }

    public void setGridPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue,Object> values) {

        grid.setPropertyValues(property, values);
    }

    public void changeGridOrder(ClientCell property, Order modiType) throws IOException {
        grid.changeGridOrder(property, modiType);
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

    public List<ClientPropertyDraw> getProperties() {
        return logicsSupplier.getProperties();
    }

    public List<ClientCell> getCells() {
        return logicsSupplier.getCells();
    }

    public ClientGroupObject getGroupObject() {
        return groupObject;
    }

    public List<ClientPropertyDraw> getGroupObjectProperties() {

        ArrayList<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();
        for (ClientPropertyDraw property : getProperties()) {
            if (groupObject.equals(property.groupObject))
                properties.add(property);
        }

        return properties;
    }

    public ClientPropertyDraw getDefaultProperty() {

        ClientCell currentCell = grid.getCurrentCell();
        if (currentCell instanceof ClientPropertyDraw)
            return (ClientPropertyDraw) currentCell;
        else
            return null;
    }

    public Object getSelectedValue(ClientPropertyDraw cell) {
        return grid.getSelectedValue(cell);
    }

    public ClientFormController getForm() {
        return form;
    }

    public String getSaveMessage() {

        String message = "";
        for (ClientObject object : groupObject) {
            if (object.addOnTransaction) {
                message += "Создать новый " + object.caption + " ?";
            }
        }

        return message;
    }
}