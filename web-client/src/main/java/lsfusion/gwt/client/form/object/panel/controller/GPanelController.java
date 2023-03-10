package lsfusion.gwt.client.form.object.panel.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.property.*;

import java.util.ArrayList;

import static java.lang.Boolean.TRUE;

public class GPanelController extends GPropertyController {

    private final NativeSIDMap<GPropertyDraw, GPropertyPanelController> propertyControllers = new NativeSIDMap<>();

    public GPanelController(GFormController formController) {
        super(formController);

        formController.addEnterBindings(GBindingMode.ALL, this::selectNextElement, null);
    }

    private void selectNextElement(boolean forward) {
        FocusUtils.focus(getNextSelectedElement(formController.getWidget().getElement(), forward), FocusUtils.Reason.KEYNEXTNAVIGATE);
    }

    private native Element getNextSelectedElement(Element formController, boolean forward) /*-{
        var elements = Array.prototype.filter.call(formController.querySelectorAll('div'), function (item) {
            return item.tabIndex >= "0"
        });
        var index = elements.indexOf($doc.activeElement);
        return forward ? (elements[index + 1] || elements[0]) : (elements[index - 1] || elements[elements.length - 1]);
    }-*/;

    @Override
    public void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setLoadings(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyPanelController propertyController = propertyControllers.get(property);
        if(!updateKeys) {
            if (propertyController == null) {
                propertyController = new GPropertyPanelController(property, formController);
                getFormLayout().addBaseComponent(property, propertyController.initView(), (FocusUtils.Reason reason) -> focusFirstWidget(reason));

                propertyControllers.put(property, propertyController);
            }
            propertyController.setColumnKeys(columnKeys);
        }
        propertyController.setPropertyValues(values, updateKeys);

        updatedProperties.put(property, Boolean.TRUE);
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        propertyControllers.remove(property);

        getFormLayout().removeBaseComponent(property);
    }

    private NativeSIDMap<GPropertyDraw, Boolean> updatedProperties = new NativeSIDMap<>();

    public void update() {
        updatedProperties.foreachKey(property -> propertyControllers.get(property).update());
        updatedProperties.clear();
    }

    public boolean isEmpty() {
        return propertyControllers.isEmpty();
    }

    public boolean containsProperty(GPropertyDraw property) {
        return propertyControllers.containsKey(property);
    }

    @Override
    public void updateCellValueElementClasses(GValueElementClassReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setCellValueElementClasses(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setCellBackgroundValues(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setCellForegroundValues(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setImages(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setPropertyCaptions(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setShowIfs(values);

        updatedProperties.put(property, TRUE); // in grid it is a little bit different (when removing showif, updatedProperties is not flagged), however it's not that important
    }

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setReadOnlyValues(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        GPropertyPanelController propertyPanelController = propertyControllers.get(propertyDraw);
        if (propertyPanelController != null) {
            propertyPanelController.focus(FocusUtils.Reason.ACTIVATE);
        }
    }

    public boolean focusFirstWidget(FocusUtils.Reason reason) {
        if (propertyControllers.isEmpty()) {
            return false;
        }

        for (GPropertyDraw property : formController.getPropertyDraws()) {
            GPropertyPanelController propController = propertyControllers.get(property);
            if (propController != null) {
                if (property.isFocusable() && propController.focus(reason)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Pair<GGroupObjectValue, Object> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, Object value) {
        GPropertyPanelController panelController = propertyControllers.get(property);
        return panelController != null ? panelController.setLoadingValueAt(fullCurrentKey, value) : null;
    }

    @Override
    public boolean isPropertyShown(GPropertyDraw property) {
        return containsProperty(property);
    }
}
