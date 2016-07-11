package lsfusion.client.form.panel;

import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.layout.ClientFormLayout;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanelController {
    private final ClientFormController form;
    private final ClientFormLayout formLayout;

    private final Map<ClientPropertyDraw, PropertyController> propertyControllers = new HashMap<>();

    private Color rowBackground;
    private Color rowForeground;


    public PanelController(ClientFormController iform, ClientFormLayout iformLayout) {
        form = iform;
        formLayout = iformLayout;
    }

    public void requestFocusInWindow() {
        // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
        for (ClientPropertyDraw property : form.getPropertyDraws()) {
            PropertyController propController = propertyControllers.get(property);
            if (propController != null && propController.requestFocusInWindow()) {
                break;
            }
        }
    }

    public void addProperty(ClientPropertyDraw property) {
        if (!containsProperty(property)) {
            PropertyController propertyController = new PropertyController(form, this, property);
            propertyControllers.put(property, propertyController);
            propertyController.addView(formLayout);
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
        if (containsProperty(property)) {
            PropertyController propController = propertyControllers.remove(property);
            propController.removeView(formLayout);
        }
    }

    public boolean containsProperty(ClientPropertyDraw property) {
        return propertyControllers.containsKey(property);
    }
    
    public PropertyController getPropertyController(ClientPropertyDraw propertyDraw) {
        return propertyControllers.get(propertyDraw);
    }

    public void update() {
        for (PropertyController propController : propertyControllers.values()) {
            propController.update(rowBackground, rowForeground);
        }
    }

    private boolean visible = true;
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            for (PropertyController propertyController : propertyControllers.values()) {
                propertyController.setVisible(visible);
            }
        }
    }

    protected void addGroupObjectActions(JComponent panelViewComponent) {
    }

    public void updateRowBackgroundValue(Color rowBackground) {
        this.rowBackground = rowBackground;
    }

    public void updateRowForegroundValue(Color rowForeground) {
        this.rowForeground = rowForeground;
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        propertyControllers.get(property).setCellBackgroundValues(cellBackgroundValues);
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        propertyControllers.get(property).setCellForegroundValues(cellForegroundValues);
    }

    public void updatePropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> valueMap, boolean updateKeys) {
        propertyControllers.get(property).setPropertyValues(valueMap, updateKeys);
    }

    public void updatePropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> propertyCaptions) {
        propertyControllers.get(property).setPropertyCaptions(propertyCaptions);
    }

    public void updateShowIfs(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> showIfs) {
        propertyControllers.get(property).setShowIfs(showIfs);
    }

    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> readOnlyValues) {
        propertyControllers.get(property).setReadOnlyValues(readOnlyValues);
    }

    public void updateColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> columnKeys) {
        propertyControllers.get(property).setColumnKeys(columnKeys);
    }
}