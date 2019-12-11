package lsfusion.client.form.object.panel.controller;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanelController {
    private final ClientFormController form;
    private final ClientFormLayout formLayout;

    private final Map<ClientPropertyDraw, PropertyPanelController> propertyControllers = new HashMap<>();

    private Color rowBackground;
    private Color rowForeground;

    public PanelController(ClientFormController iform, ClientFormLayout iformLayout) {
        form = iform;
        formLayout = iformLayout;
    }

    public void requestFocusInWindow() {
        // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
        for (ClientPropertyDraw property : form.getPropertyDraws()) {
            PropertyPanelController propController = propertyControllers.get(property);
            if (propController != null && propController.requestFocusInWindow()) {
                break;
            }
        }
    }

    public void addProperty(ClientPropertyDraw property) {
        if (!containsProperty(property)) {
            PropertyPanelController propertyPanelController = new PropertyPanelController(form, this, property);
            propertyControllers.put(property, propertyPanelController);
            propertyPanelController.addView(formLayout);
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
        if (containsProperty(property)) {
            PropertyPanelController propController = propertyControllers.remove(property);
            propController.removeView(formLayout);
        }
    }

    public boolean containsProperty(ClientPropertyDraw property) {
        return propertyControllers.containsKey(property);
    }
    
    public PropertyPanelController getPropertyController(ClientPropertyDraw propertyDraw) {
        return propertyControllers.get(propertyDraw);
    }

    public void update() {
        for (PropertyPanelController propController : propertyControllers.values()) {
            propController.update(rowBackground, rowForeground);
        }
    }

    private boolean visible = true;
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            for (PropertyPanelController propertyPanelController : propertyControllers.values()) {
                propertyPanelController.setVisible(visible);
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

    public void updateShowIfValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> showIfs) {
        propertyControllers.get(property).setShowIfs(showIfs);
    }

    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> readOnlyValues) {
        propertyControllers.get(property).setReadOnlyValues(readOnlyValues);
    }

    public void updateColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> columnKeys) {
        propertyControllers.get(property).setColumnKeys(columnKeys);
    }
}