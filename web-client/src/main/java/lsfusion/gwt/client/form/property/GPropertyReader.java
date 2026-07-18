package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.io.Serializable;

public interface GPropertyReader extends Serializable, HasNativeSID {
    void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys);

    // the data.meta field name this reader's value is projected into (react-owned properties), or null when the reader is
    // not a display-presentation reader (then it is skipped by GReactFormData.buildPropMeta)
    default String getMetaField() { return null; }
    default String getMetaField(PValue value) { return getMetaField(); }
    // how GReactFormData.convert turns this reader's value into the projected meta value
    default GMetaConverter getMetaConverter() { return GMetaConverter.STRING; }

    // COLUMN-level readers (caption / footer and a property's image) describe the whole column: projected
    // ONCE into node.meta[prop] (read at the column key), not per row; the rest (readOnly/colors/...) are per-cell (row.meta).
    // getColumnStatic is the static design fallback the owner draw supplies for this column field (else null).
    default boolean isColumnLevel(GPropertyDraw draw) { return false; }
    default String getColumnStatic(GComponent owner) { return null; }
}
