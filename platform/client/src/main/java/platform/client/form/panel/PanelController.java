package platform.client.form.panel;

import platform.client.form.PropertiesController;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.PropertyController;
import platform.client.form.cell.RemappedPropertyController;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.util.*;

public abstract class PanelController extends PropertiesController {
    private ClientFormController form;
    private GroupObjectLogicsSupplier logicsSupplier;
    private ClientFormLayout formLayout;
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyController>> controllers = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyController>>();

    private Set<ClientPropertyDraw> properties = new HashSet<ClientPropertyDraw>();

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    public PanelController(GroupObjectLogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout iformLayout) {
        logicsSupplier = ilogicsSupplier;
        form = iform;
        formLayout = iformLayout;
    }

    public void requestFocusInWindow() {
        // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
        for (ClientPropertyDraw property : logicsSupplier.getProperties()) {
            Map<ClientGroupObjectValue, PropertyController> propControllers = controllers.get(property);
            if (propControllers != null && !propControllers.isEmpty()) {
                propControllers.values().iterator().next().getView().requestFocusInWindow();
            }
        }
    }

    public void addProperty(ClientPropertyDraw property) {
        properties.add(property);
    }

    public void setPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> pvalues) {
        values.put(property, pvalues);
    }

    public void removeProperty(ClientPropertyDraw property) {
        properties.remove(property);
        removeView(property);
    }

    private void removeView(ClientPropertyDraw property) {
        Map<ClientGroupObjectValue, PropertyController> propControllers = controllers.get(property);
        if (propControllers != null) {
            for (Map.Entry<ClientGroupObjectValue, PropertyController> entry : propControllers.entrySet()) {
                entry.getValue().removeView(formLayout);
            }
            controllers.remove(property);
        }
    }

    protected abstract void addGroupObjectActions(JComponent comp);

    public void hideViews() {
        for (Map<ClientGroupObjectValue, PropertyController> propControllers : controllers.values()) {
            for (PropertyController controller : propControllers.values()) {
                controller.hideViews();
            }
        }
    }

    public void showViews() {
        for (Map<ClientGroupObjectValue, PropertyController> propControllers : controllers.values()) {
            for (PropertyController controller : propControllers.values()) {
                controller.showViews();
            }
        }
    }

    public void update() {
        for (ClientPropertyDraw property : properties) {
            List<ClientGroupObjectValue> keys = columnKeys.get(property);
            if (keys != null) {
                removeView(property);

                for (ClientGroupObjectValue columnKey : columnKeys.get(property)) {
                    updateController(property, columnKey);
                }
            } else {
                updateController(property, new ClientGroupObjectValue());
            }
        }
    }

    private void updateController(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        Map<ClientGroupObjectValue, PropertyController> propControllers = controllers.get(property);
        if (propControllers == null) {
            propControllers = new HashMap<ClientGroupObjectValue, PropertyController>();
            controllers.put(property, propControllers);
        }

        Map<ClientGroupObjectValue, Object> propValues = values.get(property);

        PropertyController propController = propControllers.get(columnKey);
        Object value = propValues.get(columnKey);
        if (propController == null && (!property.autoHide || value != null)) {
            propController = columnKey.isEmpty()
                             ? new PropertyController(property, form)
                             : new RemappedPropertyController(property, form, columnKey);
            addGroupObjectActions(propController.getView());
            propController.addView(formLayout);

            propControllers.put(columnKey, propController);
        } else if (property.autoHide && propController != null && value == null) {
            propController.removeView(formLayout);
            propControllers.remove(columnKey);
        }

        if (propController != null) {
            propController.setValue(value);
        }

        if (propController instanceof RemappedPropertyController) {
            ((RemappedPropertyController)propController).setDisplayValues(columnDisplayValues);
        }
     }
}