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
        Element nextFocusElement = getNextFocusElement(formController.getWidget().getElement(), forward);
        if(nextFocusElement != null) {
            FocusUtils.focus(nextFocusElement, FocusUtils.Reason.KEYNEXTNAVIGATE);
        }
    }

    public static native Element getNextFocusElement(Element formController, boolean forward) /*-{
        var elements = Array.prototype.filter.call(formController.querySelectorAll('tableContainer,button,input'), function (item) {
            return item.tabIndex >= "0"
        });
        if(elements.length === 0)
            return null;
        var index = elements.indexOf($doc.activeElement);
        return forward ? (elements[index + 1] || elements[0]) : (elements[index - 1] || elements[elements.length - 1]);
    }-*/;

    @Override
    public void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setLoadings(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
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
    public void updateCellValueElementClasses(GValueElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setCellValueElementClasses(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateCellCaptionElementClasses(GCaptionElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setCellCaptionElementClasses(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setCellBackgroundValues(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setCellForegroundValues(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setImages(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setPropertyCaptions(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setShowIfs(values);

        updatedProperties.put(property, TRUE); // in grid it is a little bit different (when removing showif, updatedProperties is not flagged), however it's not that important
    }

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setReadOnlyValues(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updatePropertyComments(GCommentReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setPropertyComments(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateCellCommentElementClasses(GCommentElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setCellCommentElementClasses(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updatePropertyPlaceholders(GPlaceholderReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        GPropertyDraw property = formController.getProperty(reader.propertyID);
        propertyControllers.get(property).setPropertyPlaceholders(values);

        updatedProperties.put(property, TRUE);
    }

    @Override
    public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
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
    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        GPropertyPanelController panelController = propertyControllers.get(property);
        return panelController != null ? panelController.setLoadingValueAt(fullCurrentKey, value) : null;
    }

    @Override
    public boolean isPropertyShown(GPropertyDraw property) {
        return containsProperty(property);
    }
}
