package platform.client.form.panel;

import platform.base.BaseUtils;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.form.cell.PropertyController;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.util.*;

public abstract class PanelController {
    private ClientFormController form;
    private GroupObjectLogicsSupplier logicsSupplier;
    private ClientFormLayout formLayout;

    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyController>> properties = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyController>>();

    public PanelController(GroupObjectLogicsSupplier ilogicsSupplier, ClientFormController iform, ClientFormLayout iformLayout) {
        logicsSupplier = ilogicsSupplier;
        form = iform;
        formLayout = iformLayout;
    }

    public void requestFocusInWindow() {
        // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
        for (ClientPropertyDraw property : logicsSupplier.getPropertyDraws()) {
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(property);
            if (propControllers!=null && !propControllers.isEmpty()) {
                propControllers.values().iterator().next().getView().requestFocusInWindow();
            }
        }
    }

    public void addProperty(ClientPropertyDraw property) {
        if(!properties.containsKey(property)) // так как вызывается в addDrawProperty, без проверки было свойство в панели или нет
            properties.put(property, new HashMap<ClientGroupObjectValue, PropertyController>());
    }

    public void removeProperty(ClientPropertyDraw property) {
        if(properties.containsKey(property)) // так как вызывается в addDrawProperty, без проверки было свойство в панели или нет
            for (PropertyController controller : properties.remove(property).values())
                controller.removeView(formLayout);

        columnKeys.remove(property);
        captions.remove(property);
        values.remove(property);
    }

    protected abstract void addGroupObjectActions(JComponent comp);

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

    public void setHighlight(Object value) {
        for (Map<ClientGroupObjectValue, PropertyController> propControllers : properties.values()) {
            for (PropertyController controller : propControllers.values()) {
                controller.setHighlight(value);
            }
        }
    }

    public void update() {
        for (Map.Entry<ClientPropertyDraw,Map<ClientGroupObjectValue,Object>> entry : values.entrySet()) {
            ClientPropertyDraw property = entry.getKey();
            Map<ClientGroupObjectValue, Object> propertyCaptions = captions.get(property);
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(property);

            Collection<ClientGroupObjectValue> drawKeys = new ArrayList<ClientGroupObjectValue>(); // чисто из-за autohide
            for (ClientGroupObjectValue columnKey : columnKeys.get(property)) { // именно по columnKeys чтобы сохранить порядок
                Object value = entry.getValue().get(columnKey);

                if (!(property.autoHide && value == null) // если не прятать при значении null
                    && !(propertyCaptions!=null && propertyCaptions.get(columnKey)==null)) // и если значения propertyCaption != null
                {
                    PropertyController propController = propControllers.get(columnKey);                    
                    if (propController == null) {
                        propController = new PropertyController(property, form, logicsSupplier.getGroupObject(), columnKey);
                        addGroupObjectActions(propController.getView());
                        propController.addView(formLayout);

                        propControllers.put(columnKey, propController);
                    }

                    propController.setValue(value);

                    drawKeys.add(columnKey);
                }
            }

            Iterator<Map.Entry<ClientGroupObjectValue,PropertyController>> it = propControllers.entrySet().iterator();
            while(it.hasNext()) { // удаляем те которые есть, но не нужны
                Map.Entry<ClientGroupObjectValue,PropertyController> propEntry = it.next();
                if(!drawKeys.contains(propEntry.getKey())) {
                    propEntry.getValue().removeView(formLayout);
                    it.remove();
                }
            }
        }

        // там с updateCaptions гипотетически может быть проблема если при чтении captions изменятся ключи и нарушится целостность, но это локальный баг его можно позже устранить
        for (Map.Entry<ClientPropertyDraw,Map<ClientGroupObjectValue,Object>> updateCaption : captions.entrySet()) {
            Map<ClientGroupObjectValue, PropertyController> propControllers = properties.get(updateCaption.getKey());
            for(Map.Entry<ClientGroupObjectValue,Object> updateKeys : updateCaption.getValue().entrySet()) {
                PropertyController propController = propControllers.get(updateKeys.getKey());
                if(propController!=null) // так как может быть autoHide'ута
                    propController.setCaption(BaseUtils.nullToString(updateKeys.getValue()));
            }
        }

        if (updateHighlight) {
            setHighlight(highlight);
        }

        updateHighlight = false;
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

    private boolean updateHighlight = false;
    private Object highlight;
    public void updateHighlightValue(Object highlight) {
        updateHighlight = true;
        this.highlight = highlight;
    }
}