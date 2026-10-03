package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GComponentLabelReader;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.PValue;

// THE FORM'S CONTROLLER OF A COMPONENT, settled once where the design places it (GFormController.componentControllers):
// the platform's, which draws it (GLayoutController), or what React has of one it draws or places - a container,
// a grid, a tree, a toolbar: what each reader of the component is goes to it. A property is no owner here beyond its
// class, the platform's to apply (GReactFormData.createComponentController): all React has of a property is its
// property controller's (GPropertyController)
public interface GComponentController {
    // what labels it: a container's caption, its image
    void updateLabel(GComponentLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
    // its SHOWIF
    default void updateShowIf(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        updateOther(reader, values, partial);
    }
    // any other reader of it - an element class, a value class, a custom design
    void updateOther(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial);
}
