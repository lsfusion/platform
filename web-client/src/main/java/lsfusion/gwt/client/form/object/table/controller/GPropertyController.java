package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GExtraLabelReader;
import lsfusion.gwt.client.form.property.GExtraPropertyReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

// THE FORM'S CONTROLLER OF A PROPERTY, settled once where the design says who draws it
// (GFormController.propertyControllers): the platform's controller of it - a panel's, a table's - or what React has of
// one it draws or labels. All the form sends for the property goes to it - its values, what labels it, what each of its
// other readers is, its drop
public interface GPropertyController {
    boolean isPropertyShown(GPropertyDraw property);
    void focusProperty(GPropertyDraw property);
    Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value);
    // its own values, the property being the reader of them
    void updateValue(GPropertyDraw property, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
    // what labels it: its caption, image, comment, footer (GExtraLabelReader)
    void updateLabel(GExtraLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
    // any other attribute it is drawn with - a reader with a field of its own (GExtraAttributeReader)
    default void updateAttribute(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        updateOther(reader, values, partial);
    }
    // any other reader of it
    void updateOther(GExtraPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
    // the server stopped showing it
    void dropProperty(GPropertyDraw property);
}
