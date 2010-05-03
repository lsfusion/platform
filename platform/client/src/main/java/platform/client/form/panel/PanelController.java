package platform.client.form.panel;

import platform.client.logics.*;
import platform.client.form.ClientForm;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.ClientFormLayout;

import javax.swing.*;
import java.util.Map;
import java.util.HashMap;

public abstract class PanelController {

    final Map<ClientCellView, PanelCellController> controllers = new HashMap<ClientCellView, PanelCellController>();

    ClientForm form;

    GroupObjectLogicsSupplier logicsSupplier;

    ClientFormLayout formLayout;

    public PanelController(GroupObjectLogicsSupplier ilogicsSupplier, ClientForm iform, ClientFormLayout iformLayout) {

        logicsSupplier = ilogicsSupplier;
        form = iform;
        formLayout = iformLayout;
    }

    public void addGroupObjectCells() {

        for (ClientObjectImplementView object : logicsSupplier.getGroupObject()) {

            if(object.objectCellView.show) {

                PanelCellController idController = new PanelCellController(object.objectCellView, form);
                addGroupObjectActions(idController.getView());
                idController.addView(formLayout);

                controllers.put(object.objectCellView, idController);
            }

            if(object.classCellView.show) {

                PanelCellController classController = new PanelCellController(object.classCellView, form);
                addGroupObjectActions(classController.getView());
                classController.addView(formLayout);

                controllers.put(object.classCellView, classController);
            }

        }

        if (currentObject != null)
            setGroupObjectIDValue(currentObject);

    }

    public void removeGroupObjectCells() {

        for (ClientObjectImplementView object : logicsSupplier.getGroupObject()) {
            if(object.objectCellView.show) {
                PanelCellController idController = controllers.get(object.objectCellView);
                if (idController != null) {
                    idController.removeView(formLayout);
                    controllers.remove(object.objectCellView);
                }
            }
            if(object.classCellView.show) {
                PanelCellController classController = controllers.get(object.classCellView);
                if (classController != null) {
                    classController.removeView(formLayout);
                    controllers.remove(object.classCellView);
                }
            }
        }
    }

    public void requestFocusInWindow() {

        // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectCellView.show) {
                PanelCellController idController = controllers.get(object.objectCellView);
                if (idController != null) {
                    idController.getView().requestFocusInWindow();
                    return;
                }
            }
    }

    private void setGroupObjectIDValue(ClientGroupObjectValue value) {

        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectCellView.show) {
                PanelCellController idcontroller = controllers.get(object.objectCellView);
                if (idcontroller != null)
                    idcontroller.setValue(value.get(object));
            }
    }

    public void setCurrentClass(ClientGroupObjectClass value) {

        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.classCellView.show) {
                PanelCellController classController = controllers.get(object.classCellView);
                if (classController != null)
                    classController.setValue(value.get(object));
            }
    }

    ClientGroupObjectValue currentObject;

    public void selectObject(ClientGroupObjectValue value) {

        currentObject = value;
        setGroupObjectIDValue(value);
    }

    public void addProperty(ClientPropertyView property) {

        if (controllers.get(property) == null) {

            PanelCellController propController = new PanelCellController(property, form);
            addGroupObjectActions(propController.getView());
            propController.addView(formLayout);

            controllers.put(property, propController);
        }

    }

    public void removeProperty(ClientPropertyView property) {

        PanelCellController propController = controllers.get(property);
        if (propController != null) {
            propController.removeView(formLayout);
            controllers.remove(property);
        }

    }

    public void setPropertyValue(ClientPropertyView property, Object value) {

        PanelCellController propmodel = controllers.get(property);
        propmodel.setValue(value);

    }

    protected abstract void addGroupObjectActions(JComponent comp);

    public void hideViews() {

        for (ClientCellView property : controllers.keySet()) {
            controllers.get(property).hideViews();
        }
    }

    public void showViews() {

        for (ClientCellView property : controllers.keySet()) {
            controllers.get(property).showViews();
        }
    }
}