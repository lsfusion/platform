package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.*;

import java.util.ArrayList;

public abstract class GPropertyController {

    protected final GFormController formController;

    public GPropertyController(GFormController formController) {
        this.formController = formController;
    }

    protected GFormLayout getFormLayout() {
        return formController.formLayout;
    }

    public abstract void updateCellValueElementClasses(GValueElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellCaptionElementClasses(GCaptionElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updatePropertyComments(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellCommentElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updatePlaceholderValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updatePatternValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateRegexpValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateRegexpMessageValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateValueTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, PValue> values);

    public abstract boolean isPropertyShown(GPropertyDraw property);
    public abstract void focusProperty(GPropertyDraw propertyDraw);

    public abstract void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void removeProperty(GPropertyDraw property);

    public abstract Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value);
}
