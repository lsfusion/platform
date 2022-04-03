package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.*;

import java.util.ArrayList;
import java.util.Optional;

public abstract class GPropertyController {

    protected final GFormController formController;

    public GPropertyController(GFormController formController) {
        this.formController = formController;
    }

    protected GFormLayout getFormLayout() {
        return formController.formLayout;
    }

    public abstract void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, Object> values);

    public abstract boolean isPropertyShown(GPropertyDraw property);
    public abstract void focusProperty(GPropertyDraw propertyDraw);

    public abstract void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void removeProperty(GPropertyDraw property);

    public abstract Optional<Object> setValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, Object value);
}
