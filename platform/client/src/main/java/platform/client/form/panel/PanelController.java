package platform.client.form.panel;

import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.PropertyController;
import platform.client.logics.*;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public abstract class PanelController {

    final Map<ClientPropertyDraw, PropertyController> controllers = new HashMap<ClientPropertyDraw, PropertyController>();

    ClientFormController form;

    GroupObjectLogicsSupplier logicsSupplier;

    ClientFormLayout formLayout;

    public PanelController(GroupObjectLogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout iformLayout) {

        logicsSupplier = ilogicsSupplier;
        form = iform;
        formLayout = iformLayout;
    }

    public void requestFocusInWindow() {

        // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
        for(ClientPropertyDraw property : logicsSupplier.getProperties()) {
            PropertyController propController = controllers.get(property);
            if(propController != null) {
                propController.getView().requestFocusInWindow();
                return;
            }
        }
    }

    public void addProperty(ClientPropertyDraw property, Object value) {

        PropertyController controller = controllers.get(property);
        if (controller == null && (!property.autoHide || value != null)) {

            PropertyController propController = new PropertyController(property, form);
            addGroupObjectActions(propController.getView());
            propController.addView(formLayout);

            controllers.put(property, propController);
        } else if (property.autoHide && controller != null && value == null) {
            removeProperty(property);
        }

    }

    public void removeProperty(ClientPropertyDraw property) {

        PropertyController propController = controllers.get(property);
        if (propController != null) {
            propController.removeView(formLayout);
            controllers.remove(property);
        }

    }

    public void setPropertyValue(ClientPropertyDraw property, Object value) {
        PropertyController propmodel = controllers.get(property);
        if (propmodel != null) {
            propmodel.setValue(value);
        }
    }

    protected abstract void addGroupObjectActions(JComponent comp);

    public void hideViews() {

        for (ClientPropertyDraw property : controllers.keySet()) {
            controllers.get(property).hideViews();
        }
    }

    public void showViews() {

        for (ClientPropertyDraw property : controllers.keySet()) {
            controllers.get(property).showViews();
        }
    }
}