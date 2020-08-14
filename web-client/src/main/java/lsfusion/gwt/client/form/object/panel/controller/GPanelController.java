package lsfusion.gwt.client.form.object.panel.controller;

import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.property.*;

import java.util.ArrayList;

public class GPanelController extends GPropertyController {

    private final GFormController formController;

    private final NativeSIDMap<GPropertyDraw, GPropertyPanelController> propertyControllers = new NativeSIDMap<>();

    private Object rowBackground;
    private Object rowForeground;

    public GPanelController(GFormController iform) {
        this.formController = iform;
    }

    private GFormLayout getFormLayout() {
        return formController.formLayout;
    }

    @Override
    public void updateProperty(GGroupObject group, GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyPanelController propertyController = propertyControllers.get(property);
        if(!updateKeys) {
            if (propertyController == null) {
                propertyController = new GPropertyPanelController(property, formController, () -> rowBackground, () -> rowForeground);
                propertyControllers.put(property, propertyController);

                getFormLayout().addBaseComponent(property, propertyController.getView(), this::focusFirstWidget);
            }
            propertyController.setColumnKeys(columnKeys);
        }
        propertyController.setPropertyValues(values, updateKeys);
    }

    @Override
    public void removeProperty(GGroupObject group, GPropertyDraw property) {
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

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        if(values != null && !values.isEmpty()) {
            rowBackground = values.firstValue();
        }
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        if(values != null && !values.isEmpty()) {
            rowForeground = values.firstValue();
        }
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (!property.grid) {
            propertyControllers.get(property).setCellBackgroundValues(values);
        }
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (!property.grid) {
            propertyControllers.get(property).setCellForegroundValues(values);
        }
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (!property.grid) {
            propertyControllers.get(property).setImages(values);
        }
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (!property.grid) {
            propertyControllers.get(property).setPropertyCaptions(values);
        }
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (!property.grid) {
            propertyControllers.get(property).setShowIfs(values);
        }
    }

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (!property.grid) {
            propertyControllers.get(property).setReadOnlyValues(values);
        }
    }

    @Override
    public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        propertyControllers.get(propertyDraw).focusFirstWidget();
    }

    public boolean focusFirstWidget() {
        if (propertyControllers.isEmpty()) {
            return false;
        }

        for (GPropertyDraw property : formController.getPropertyDraws()) {
            GPropertyPanelController propController = propertyControllers.get(property);
            if (propController != null) {
                if (propController.focusFirstWidget()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void setContainerCaption(GContainer container, String caption) {
        formController.setContainerCaption(container, caption);
    }

    public void processFormChanges(GFormChanges fc, NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects) {
        processFormChanges(null, fc, currentGridObjects,
                property -> !property.grid && containsProperty(property),
                property -> !property.grid,
                propertyReader -> true);

        update();
        setVisible(true);
    }
}
