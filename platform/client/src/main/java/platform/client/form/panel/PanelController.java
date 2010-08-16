package platform.client.form.panel;

import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.CellController;
import platform.client.logics.*;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public abstract class PanelController {

    final Map<ClientCell, CellController> controllers = new HashMap<ClientCell, CellController>();

    ClientFormController form;

    GroupObjectLogicsSupplier logicsSupplier;

    ClientFormLayout formLayout;

    public PanelController(GroupObjectLogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout iformLayout) {

        logicsSupplier = ilogicsSupplier;
        form = iform;
        formLayout = iformLayout;
    }

    public void addGroupObjectCells() {

        for (ClientObject object : logicsSupplier.getGroupObject()) {

            if(object.objectIDCell.show) {

                CellController idController = new CellController(object.objectIDCell, form);
                addGroupObjectActions(idController.getView());
                idController.addView(formLayout);

                controllers.put(object.objectIDCell, idController);
            }

            if(object.classCell.show) {

                CellController classController = new CellController(object.classCell, form);
                addGroupObjectActions(classController.getView());
                classController.addView(formLayout);

                controllers.put(object.classCell, classController);
            }

        }

        if (currentObject != null)
            setGroupObjectIDValue(currentObject);

    }

    public void removeGroupObjectCells() {

        for (ClientObject object : logicsSupplier.getGroupObject()) {
            if(object.objectIDCell.show) {
                CellController idController = controllers.get(object.objectIDCell);
                if (idController != null) {
                    idController.removeView(formLayout);
                    controllers.remove(object.objectIDCell);
                }
            }
            if(object.classCell.show) {
                CellController classController = controllers.get(object.classCell);
                if (classController != null) {
                    classController.removeView(formLayout);
                    controllers.remove(object.classCell);
                }
            }
        }
    }

    public void requestFocusInWindow() {

        // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
        for (ClientObject object : logicsSupplier.getGroupObject())
            if(object.objectIDCell.show) {
                CellController idController = controllers.get(object.objectIDCell);
                if (idController != null) {
                    idController.getView().requestFocusInWindow();
                    return;
                }
            }
    }

    private void setGroupObjectIDValue(ClientGroupObjectValue value) {

        for (ClientObject object : logicsSupplier.getGroupObject())
            if(object.objectIDCell.show) {
                CellController idcontroller = controllers.get(object.objectIDCell);
                if (idcontroller != null)
                    idcontroller.setValue(value.get(object));
            }
    }

    public void setCurrentClass(ClientGroupObjectClass value) {

        for (ClientObject object : logicsSupplier.getGroupObject())
            if(object.classCell.show) {
                CellController classController = controllers.get(object.classCell);
                if (classController != null)
                    classController.setValue(value.get(object));
            }
    }

    ClientGroupObjectValue currentObject;

    public void selectObject(ClientGroupObjectValue value) {

        currentObject = value;
        setGroupObjectIDValue(value);
    }

    public void addProperty(ClientPropertyDraw property, Object value) {

        CellController controller = controllers.get(property);
        if (controller == null && (!property.autoHide || value != null)) {

            CellController propController = new CellController(property, form);
            addGroupObjectActions(propController.getView());
            propController.addView(formLayout);

            controllers.put(property, propController);
        } else if (property.autoHide && controller != null && value == null) {
            removeProperty(property);
        }

    }

    public void removeProperty(ClientPropertyDraw property) {

        CellController propController = controllers.get(property);
        if (propController != null) {
            propController.removeView(formLayout);
            controllers.remove(property);
        }

    }

    public void setPropertyValue(ClientPropertyDraw property, Object value) {
        CellController propmodel = controllers.get(property);
        if (propmodel != null) {
            propmodel.setValue(value);
        }
    }

    protected abstract void addGroupObjectActions(JComponent comp);

    public void hideViews() {

        for (ClientCell property : controllers.keySet()) {
            controllers.get(property).hideViews();
        }
    }

    public void showViews() {

        for (ClientCell property : controllers.keySet()) {
            controllers.get(property).showViews();
        }
    }
}