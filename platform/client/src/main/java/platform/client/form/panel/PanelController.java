package platform.client.form.panel;

import platform.base.BaseUtils;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.PropertyController;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.util.*;

public class PanelController {
    private ClientFormController form;
    private GroupObjectLogicsSupplier logicsSupplier;
    private ClientFormLayout formLayout;

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyController>> properties = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyController>>();

    public PanelController(GroupObjectLogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout iformLayout) {
        logicsSupplier = ilogicsSupplier;
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
                if (property.panelLocation != null)
                    removePropertyFromPanelLocation(controller);
                controller.removeView(formLayout);
            }
        }

        columnKeys.remove(property);
        captions.remove(property);
        cellHighlights.remove(property);
        values.remove(property);
    }

    protected void addGroupObjectActions(JComponent comp) {
        //do nothing by default
    }

    public void hideViews() {
        for (Map<ClientGroupObjectValue, PropertyController> propControllers : properties.values()) {
            for (PropertyController controller : propControllers.values()) {
                controller.hideViews();
            }
        }
    }

    public void showViews() {
        for (Map<ClientGroupObjectValue, PropertyController> propControllers : properties.values()) {
            for (PropertyController controller : propControllers.values()) {
                controller.showViews();
            }
        }
    }

    public void setRowHighlight(Object value) {
        for (Map<ClientGroupObjectValue, PropertyController> propControllers : properties.values()) {
            for (PropertyController controller : propControllers.values()) {
                ClientGroupObject groupObject = logicsSupplier.getSelectedGroupObject();
                controller.setHighlight(value, groupObject == null ? null : groupObject.highlightColor);
            }
        }
    }

    private void addPropertyToPanelLocation(PropertyController controller) {
        GroupObjectLogicsSupplier logicsSupplier = form.getGroupObjectLogicsSupplier(controller.getKey().groupObject);
        if (logicsSupplier != null) {
            if (controller.getKey().panelLocation.isToolbarLocation()) {
                logicsSupplier.addPropertyToToolbar(controller);
                logicsSupplier.updateToolbar();
            } else {
                logicsSupplier.addPropertyToShortcut(controller);
            }
        }
    }

    private void removePropertyFromPanelLocation(PropertyController controller) {
        GroupObjectLogicsSupplier logicsSupplier = form.getGroupObjectLogicsSupplier(controller.getKey().groupObject);
        if (logicsSupplier != null) {
            if (controller.getKey().panelLocation.isToolbarLocation()) {
                logicsSupplier.removePropertyFromToolbar(controller);
                logicsSupplier.updateToolbar();
            } else {
                logicsSupplier.removePropertyFromShortcut(controller);
            }
        }
    }

    public void update() {
        for (Map.Entry<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> entry : values.entrySet()) {
            ClientPropertyDraw property = entry.getKey();
            Map<ClientGroupObjectValue, Object> propertyCaptions = captions.get(property);
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(property);

            Collection<ClientGroupObjectValue> drawKeys = new ArrayList<ClientGroupObjectValue>(); // чисто из-за autohide
            for (ClientGroupObjectValue columnKey : columnKeys.get(property)) { // именно по columnKeys чтобы сохранить порядок
                Object value = entry.getValue().get(columnKey);

                if (!(property.autoHide && value == null) // если не прятать при значении null
                        && !(propertyCaptions != null && propertyCaptions.get(columnKey) == null)) // и если значения propertyCaption != null
                {
                    PropertyController propController = propControllers.get(columnKey);
                    if (propController == null) {
                        propController = new PropertyController(property, form, columnKey);
                        addGroupObjectActions(propController.getView());
                        if (property.panelLocation != null && property.groupObject != null) {
                            addPropertyToPanelLocation(propController);
                        } else {
                            propController.addView(formLayout);
                        }

                        propControllers.put(columnKey, propController);
                    }

                    propController.setValue(value);

                    drawKeys.add(columnKey);
                }
            }

            Iterator<Map.Entry<ClientGroupObjectValue, PropertyController>> it = propControllers.entrySet().iterator();
            while (it.hasNext()) { // удаляем те которые есть, но не нужны
                Map.Entry<ClientGroupObjectValue, PropertyController> propEntry = it.next();
                if (!drawKeys.contains(propEntry.getKey())) {
                    if (property.panelLocation != null && property.groupObject != null) {
                        removePropertyFromPanelLocation(propEntry.getValue());
                    } else
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
                    propController.setCaption(BaseUtils.toCaption(updateKeys.getValue()));
                }
            }
        }

        logicsSupplier.updateToolbar();

        setRowHighlight(rowHighlight);

        for (Map.Entry<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> updateCellHighlights : cellHighlights.entrySet()) {
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(updateCellHighlights.getKey());
            for (Map.Entry<ClientGroupObjectValue, Object> updateKeys : updateCellHighlights.getValue().entrySet()) {
                PropertyController propController = propControllers.get(updateKeys.getKey());
                // так как может быть autoHide'ута
                if (propController != null && rowHighlight == null) {
                    propController.setHighlight(updateKeys.getValue(), updateCellHighlights.getKey().highlightColor);
                }
            }
        }
    }

    protected Map<ClientPropertyDraw, List<ClientGroupObjectValue>> columnKeys = new HashMap<ClientPropertyDraw, List<ClientGroupObjectValue>>();

    public void updateColumnKeys(ClientPropertyDraw property, List<ClientGroupObjectValue> groupColumnKeys) {
        columnKeys.put(property, groupColumnKeys);
    }

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    public void updatePropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> pvalues) {
        values.put(property, pvalues);
    }

    protected Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> captions = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    public void updatePropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        this.captions.put(property, captions);
    }

    protected Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> cellHighlights = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    public void updateCellHighlightValue(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellHighlights) {
        this.cellHighlights.put(property, cellHighlights);
    }

    private Object rowHighlight;

    public void updateRowHighlightValue(Object rowHighlight) {
        this.rowHighlight = rowHighlight;
    }
}