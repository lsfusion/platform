package lsfusion.gwt.client.form.object.panel.controller;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;

public class GPanelController {

    private final GFormController form;

    private final NativeSIDMap<GPropertyDraw, GPropertyPanelController> propertyControllers = new NativeSIDMap<>();

    private Object rowBackground;
    private Object rowForeground;

    public GPanelController(GFormController iform) {
        this.form = iform;
    }

    private GFormLayout getFormLayout() {
        return form.formLayout;
    }

    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyPanelController propertyController = propertyControllers.get(property);
        if(!updateKeys) {
            if (propertyController == null) {
                propertyController = new GPropertyPanelController(property, form, () -> rowBackground, () -> rowForeground);
                propertyControllers.put(property, propertyController);

                getFormLayout().addBaseComponent(property, propertyController.getView(), this::focusFirstWidget);
            }
            propertyController.setColumnKeys(columnKeys);
        }
        propertyController.setPropertyValues(values, updateKeys);
    }

    public void removeProperty(GPropertyDraw property) {
        GPropertyPanelController propController = propertyControllers.remove(property);

        getFormLayout().removeBaseComponent(property, propController.getView());
    }

    public void update() {
        propertyControllers.foreachValue(GPropertyPanelController::update);
    }

    public boolean isEmpty() {
        return propertyControllers.isEmpty();
    }

    private boolean visible = true;
    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            propertyControllers.foreachValue(propertyController -> propertyController.getView().setVisible(visible));
        }
    }

    public boolean containsProperty(GPropertyDraw property) {
        return propertyControllers.containsKey(property);
    }

    public void updateRowBackgroundValue(Object color) {
        rowBackground = color;
    }

    public void updateRowForegroundValue(Object color) {
        rowForeground = color;
    }

    public void updateCellBackgroundValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> cellBackgroundValues) {
        propertyControllers.get(property).setCellBackgroundValues(cellBackgroundValues);
    }

    public void updateCellForegroundValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> cellForegroundValues) {
        propertyControllers.get(property).setCellForegroundValues(cellForegroundValues);
    }

    public void updateCellImages(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> imageValues) {
        propertyControllers.get(property).setImages(imageValues);
    }

    public void updatePropertyCaptions(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> propertyCaptions) {
        propertyControllers.get(property).setPropertyCaptions(propertyCaptions);
    }

    public void updateShowIfValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> showIfs) {
        propertyControllers.get(property).setShowIfs(showIfs);
    }

    public void updateReadOnlyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> readOnlyValues) {
        propertyControllers.get(property).setReadOnlyValues(readOnlyValues);
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        propertyControllers.get(propertyDraw).focusFirstWidget();
    }

    public boolean focusFirstWidget() {
        if (propertyControllers.isEmpty()) {
            return false;
        }

        for (GPropertyDraw property : form.getPropertyDraws()) {
            GPropertyPanelController propController = propertyControllers.get(property);
            if (propController != null) {
                if (propController.focusFirstWidget()) {
                    return true;
                }
            }
        }

        return false;
    }

}
