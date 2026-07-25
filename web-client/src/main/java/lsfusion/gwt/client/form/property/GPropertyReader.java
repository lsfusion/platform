package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.io.Serializable;

public interface GPropertyReader extends Serializable, HasNativeSID {
    void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys);

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

    // whether this reader decides that its owner's entry EXISTS at all, instead of carrying one attribute of it. A
    // presence reader has no attribute field to write, so the projection routes it before asking anything else.
    default boolean isPresenceReader() { return false; }
    default String getStaticAttribute(GComponent owner) { return null; }

    // the same question for a GROUP's own attribute: ROW = one per row (written on each row, dirties the changed rows),
    // GROUP = one for the whole group (read at EMPTY, dirties only the group). Only meaningful when the owner IS a group.
    default GGroupAttributeScope getAttributeScope() { return GGroupAttributeScope.ROW; }

    // WHAT this reader carries an attribute OF - answered by the reader, so the projection routes and invalidates it
    // without rediscovering ownership from the reader's class. Exactly one of the two answers, by where the attribute
    // belongs: a COMPONENT (a container, or a property draw) or an object GROUP. A reader that carries a VALUE rather
    // than an attribute (a property draw itself) owns nothing here and answers neither.
    default GComponent getAttributeComponent(GForm form) { return null; }
    default GGroupObject getAttributeGroup(GForm form) { return null; }

    // whether this is the owner's OWN caption / image - the pair the platform hands to React for an lsf child
    // instead of drawing it, and the only attributes a container has at all
    default boolean isDescriptorAttribute() { return false; }
}
