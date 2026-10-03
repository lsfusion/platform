package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;

// a component's reader that LABELS it - a container's caption, its image: the component's controller takes it
// (GComponentController.updateLabel), whoever draws the rest of it. A property's labels are its own (GExtraLabelReader)
public interface GComponentLabelReader extends GComponentReader {
    @Override
    default void update(GComponentController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        controller.updateLabel(this, values, partial);
    }
}
