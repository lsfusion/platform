package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.io.Serializable;

public interface GPropertyReader extends Serializable, HasNativeSID {
    // what a delta brings for it goes to the form's controller of its owner - the property, the group, the component it
    // is of, which each family of readers names (GFormController.propertyControllers / groupControllers /
    // componentControllers) - told what kind of reader it is
    void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);

    // the field name this reader's value is projected into on the property's object (react-owned properties), or null when
    // the reader is not an attribute reader (then no entry carries it)
    default String getAttributeField() { return null; }
    default String getAttributeField(PValue value) { return getAttributeField(); }
    // how this reader's delivered value is turned into the projected attribute
    default GAttributeConverter getAttributeConverter() { return GAttributeConverter.STRING; }

    // COLUMN-level readers (caption / footer and a property's image) describe the whole column: projected
    // ONCE into node.<prop> (read at the column key), not per row; the rest (readOnly/colors/...) are per-cell (row.<prop>).
    // getStaticAttribute is the static design fallback the owner draw supplies for this column field (else null).
    default boolean isColumnAttribute(GPropertyDraw draw) { return false; }

    default String getStaticAttribute(GComponent owner) { return null; }

    // the same question for a GROUP's own attribute: ROW = one per row (written on each row, dirties the changed rows),
    // GROUP = one for the whole group (read at EMPTY, dirties only the group). Only meaningful when the owner IS a group.
    default GGroupAttributeScope getAttributeScope() { return GGroupAttributeScope.ROW; }
}
