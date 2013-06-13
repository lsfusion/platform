package lsfusion.client.form.panel;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.ClientFormLayout;
import lsfusion.client.form.cell.PropertyController;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PanelController {
    private ClientFormController form;
    private ClientFormLayout formLayout;

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyController>> properties = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyController>>();

    private Map<ClientPropertyDraw, List<ClientGroupObjectValue>> columnKeys = new HashMap<ClientPropertyDraw, List<ClientGroupObjectValue>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> readOnlyValues = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> captions = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> showIfs = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> cellBackgroundValues = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> cellForegroundValues = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();
    private Color rowBackground;
    private Color rowForeground;


    public PanelController(ClientFormController iform, ClientFormLayout iformLayout) {
        form = iform;
        formLayout = iformLayout;
    }

    public Map<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyController>> getProperties() {
        return properties;
    }

    public void requestFocusInWindow() {
        // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
        for (ClientPropertyDraw property : form.getPropertyDraws()) {
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(property);
            if (propControllers != null && !propControllers.isEmpty()) {
                propControllers.values().iterator().next().getView().requestFocusInWindow();
                break;
            }
        }
    }

    public void addProperty(ClientPropertyDraw property) {
        if (!properties.containsKey(property)) {
            // так как вызывается в addDrawProperty, без проверки было свойство в панели или нет
            properties.put(property, new HashMap<ClientGroupObjectValue, PropertyController>());
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
        if (properties.containsKey(property)) {
            // так как вызывается в addDrawProperty, без проверки было свойство в панели или нет

            for (PropertyController controller : properties.remove(property).values()) {
                controller.removeView(formLayout);
            }
        }

        columnKeys.remove(property);
        captions.remove(property);
        showIfs.remove(property);
        cellBackgroundValues.remove(property);
        cellForegroundValues.remove(property);
        values.remove(property);
        readOnlyValues.remove(property);
    }

    public boolean containsProperty(ClientPropertyDraw property) {
        return properties.containsKey(property);
    }

    protected void addGroupObjectActions(JComponent comp) {
        //do nothing by default
    }

    public void setRowBackground(Color value) {
        for (Map<ClientGroupObjectValue, PropertyController> propControllers : properties.values()) {
            for (PropertyController controller : propControllers.values()) {
                controller.setBackgroundColor(value);
            }
        }
    }

    public void setRowForeground(Color value) {
        for (Map<ClientGroupObjectValue, PropertyController> propControllers : properties.values()) {
            for (PropertyController controller : propControllers.values()) {
                controller.setForegroundColor(value);
            }
        }
    }

    public void update() {
        for (Map.Entry<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> entry : values.entrySet()) {
            ClientPropertyDraw property = entry.getKey();
            Map<ClientGroupObjectValue, Object> propertyValues = entry.getValue();
            Map<ClientGroupObjectValue, Object> propertyReadOnly = readOnlyValues.get(property);
            Map<ClientGroupObjectValue, Object> propertyShowIfs = showIfs.get(property);
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(property);

            Collection<ClientGroupObjectValue> drawKeys = new ArrayList<ClientGroupObjectValue>(); // чисто из-за autohide
            for (ClientGroupObjectValue columnKey : columnKeys.get(property)) { // именно по columnKeys чтобы сохранить порядок
                Object value = propertyValues.get(columnKey);

                if (!(property.autoHide && value == null) // если не прятать при значении null
                        && !(propertyShowIfs != null && propertyShowIfs.get(columnKey) == null)) // и если значения propertyShowIf != null
                {
                    PropertyController propController = propControllers.get(columnKey);
                    if (propController == null) {
                        propController = new PropertyController(property, form, columnKey);

                        addGroupObjectActions(propController.getView());

                        propController.addView(formLayout);

                        propControllers.put(columnKey, propController);
                    }

                    propController.setValue(value);

                    propController.setReadOnly(propertyReadOnly != null && propertyReadOnly.get(columnKey) != null);

                    drawKeys.add(columnKey);
                }
            }

            Iterator<Map.Entry<ClientGroupObjectValue, PropertyController>> it = propControllers.entrySet().iterator();
            while (it.hasNext()) { // удаляем те которые есть, но не нужны
                Map.Entry<ClientGroupObjectValue, PropertyController> propEntry = it.next();
                if (!drawKeys.contains(propEntry.getKey())) {
                    propEntry.getValue().removeView(formLayout);
                    it.remove();
                }
            }
        }

        // там с updateCaptions гипотетически может быть проблема, если при чтении captions изменятся ключи и нарушится целостность,
        // но это локальный баг, его можно позже устранить
        for (Map.Entry<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> updateCaption : captions.entrySet()) {
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(updateCaption.getKey());
            for (Map.Entry<ClientGroupObjectValue, Object> updateKeys : updateCaption.getValue().entrySet()) {
                PropertyController propController = propControllers.get(updateKeys.getKey());
                // так как может быть autoHide'ута
                if (propController != null) {
                    propController.setCaption(updateCaption.getKey().getDynamicCaption(updateKeys.getValue()));
                }
            }
        }

        setRowBackground(rowBackground);
        setRowForeground(rowForeground);

        for (Map.Entry<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> updateCellBackgroundValues : cellBackgroundValues.entrySet()) {
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(updateCellBackgroundValues.getKey());
            for (Map.Entry<ClientGroupObjectValue, Object> updateKeys : updateCellBackgroundValues.getValue().entrySet()) {
                PropertyController propController = propControllers.get(updateKeys.getKey());
                // так как может быть autoHide'ута
                if (propController != null && rowBackground == null) {
                    propController.setBackgroundColor((Color) updateKeys.getValue());
                }
            }
        }

        for (Map.Entry<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> updateCellForegroundValues : cellForegroundValues.entrySet()) {
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(updateCellForegroundValues.getKey());
            for (Map.Entry<ClientGroupObjectValue, Object> updateKeys : updateCellForegroundValues.getValue().entrySet()) {
                PropertyController propController = propControllers.get(updateKeys.getKey());
                // так как может быть autoHide'ута
                if (propController != null && rowForeground == null) {
                    propController.setForegroundColor((Color) updateKeys.getValue());
                }
            }
        }
    }

    private boolean visible = true;
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            for (Map<ClientGroupObjectValue, PropertyController> propControllers : properties.values()) {
                for (PropertyController controller : propControllers.values()) {
                    controller.setVisible(visible);
                }
            }
        }
    }

    public void updateColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> groupColumnKeys) {
        columnKeys.put(property, groupColumnKeys);
    }

    public void updatePropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> pvalues, boolean update) {
        BaseUtils.putUpdate(values, property, pvalues, update);
    }


    public void updatePropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        this.captions.put(property, captions);
    }

    public void updateShowIfs(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> showIfs) {
        this.showIfs.put(property, showIfs);
    }

    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> readOnlyValues) {
        this.readOnlyValues.put(property, readOnlyValues);
    }

    public void updateCellBackgroundValue(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        this.cellBackgroundValues.put(property, cellBackgroundValues);
    }

    public void updateCellForegroundValue(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        this.cellForegroundValues.put(property, cellForegroundValues);
    }

    public void updateRowBackgroundValue(Color rowBackground) {
        this.rowBackground = rowBackground;
    }

    public void updateRowForegroundValue(Color rowForeground) {
        this.rowForeground = rowForeground;
    }
}