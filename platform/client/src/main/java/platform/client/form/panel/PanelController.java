package platform.client.form.panel;

import platform.client.logics.ClientCellView;
import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyView;
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

    public void addGroupObjectID() {

        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectIDView.show) {

                PanelCellController idController = new PanelCellController(object.objectIDView, form);
                addGroupObjectActions(idController.getView());
                idController.addView(formLayout);

                controllers.put(object.objectIDView, idController);
            }

        if (currentObject != null)
            setGroupObjectIDValue(currentObject);

    }

    public void removeGroupObjectID() {

        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectIDView.show) {
                PanelCellController idController = controllers.get(object.objectIDView);
                if (idController != null) {
                    idController.removeView(formLayout);
                    controllers.remove(object.objectIDView);
                }
            }
    }

    public void requestFocusInWindow() {

        // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectIDView.show) {
                PanelCellController idController = controllers.get(object.objectIDView);
                if (idController != null) {
                    idController.getView().requestFocusInWindow();
                    return;
                }
            }
    }

    private void setGroupObjectIDValue(ClientGroupObjectValue value) {

        for (ClientObjectImplementView object : logicsSupplier.getGroupObject())
            if(object.objectIDView.show) {
                PanelCellController idmodel = controllers.get(object.objectIDView);
                if (idmodel != null)
                    idmodel.setValue(value.get(object));
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