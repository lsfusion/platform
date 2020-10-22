package lsfusion.gwt.client.form.object.panel.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GBindingMode;
import lsfusion.gwt.client.form.event.GKeyInputEvent;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.property.*;

import java.util.ArrayList;

public class GPanelController extends GPropertyController {

    private final NativeSIDMap<GPropertyDraw, GPropertyPanelController> propertyControllers = new NativeSIDMap<>();

    public GPanelController(GFormController formController) {
        super(formController);

        addEnterBinding(true);
        addEnterBinding(false);
    }

    private void addEnterBinding(boolean shiftPressed) {
        formController.addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER, false, false, shiftPressed)),
                new GBindingEnv(-100, null, null, GBindingMode.ALL, GBindingMode.NO, null),  // bindEditing - NO, because we don't want for example when editing text in grid to catch enter
                event -> getNextSelectedElement(formController.getElement(), shiftPressed).focus(),
                null, null);
    }

    private native Element getNextSelectedElement(Element formController, boolean shiftPressed) /*-{
        var elements = Array.prototype.filter.call(formController.querySelectorAll('div'), function (item) {
            return item.tabIndex >= "0"
        });
        var index = elements.indexOf($doc.activeElement);
        return shiftPressed ? (elements[index - 1] || elements[elements.length - 1]) : (elements[index + 1] || elements[0]);
    }-*/;

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyPanelController propertyController = propertyControllers.get(property);
        if(!updateKeys) {
            if (propertyController == null) {
                propertyController = new GPropertyPanelController(property, formController);
                propertyControllers.put(property, propertyController);

                getFormLayout().addBaseComponent(property, propertyController.getView(), this::focusFirstWidget);
            }
            propertyController.setColumnKeys(columnKeys);
        }
        propertyController.setPropertyValues(values, updateKeys);
    }

    @Override
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

    public boolean containsProperty(GPropertyDraw property) {
        return propertyControllers.containsKey(property);
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        propertyControllers.get(formController.getProperty(reader.propertyID)).setCellBackgroundValues(values);
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        propertyControllers.get(formController.getProperty(reader.propertyID)).setCellForegroundValues(values);
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        propertyControllers.get(formController.getProperty(reader.propertyID)).setImages(values);
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        propertyControllers.get(formController.getProperty(reader.propertyID)).setPropertyCaptions(values);
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        propertyControllers.get(formController.getProperty(reader.propertyID)).setShowIfs(values);
    }

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        propertyControllers.get(formController.getProperty(reader.propertyID)).setReadOnlyValues(values);
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
                if (property.isFocusable() && propController.focusFirstWidget()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isPropertyShown(GPropertyDraw property) {
        return containsProperty(property);
    }
}
