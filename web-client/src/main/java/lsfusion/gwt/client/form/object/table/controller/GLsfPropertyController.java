package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.*;

import java.util.ArrayList;

public abstract class GLsfPropertyController implements GPropertyController {

    protected final GFormController formController;

    public GLsfPropertyController(GFormController formController) {
        this.formController = formController;
    }

    protected GFormLayout getFormLayout() {
        return formController.formLayout;
    }

    public abstract void updateCellGridElementClasses(GGridElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellValueElementClasses(GValueElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellCaptionElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellFooterElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellFontValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updatePropertyComments(GCommentReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateCellCommentElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updatePlaceholderValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updatePatternValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateRegexpValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateRegexpMessageValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateValueTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updatePropertyCustomOptionsValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateChangeKeyValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateChangeMouseValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateDefaultValueValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, PValue> values);

    public abstract boolean isPropertyShown(GPropertyDraw property);
    public abstract void focusProperty(GPropertyDraw propertyDraw);

    public abstract void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values);
    public abstract void removeProperty(GPropertyDraw property);

    public abstract Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value);

    // ===== the platform's side of the form's controller of a property (GPropertyController): its values into its
    // view, for the columns the grids show now; any reader of it by that reader's own update of this controller; its
    // drop - its view goes, if it made one: a drop is sent without checking that the property was ever sent for an
    // update
    public void updateValue(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        updateProperty(property, GPropertyDraw.getColumnKeys(property, formController.getCurrentGridObjects()), partial, values);
    }
    public void updateOther(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        reader.updateLsf(this, values);
    }
    public void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        reader.updateLsf(this, values);
    }
    public void dropProperty(GPropertyDraw property) {
        if (isPropertyShown(property))
            removeProperty(property);
    }
}
