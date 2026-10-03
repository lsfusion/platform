package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;

public interface GComponentReader extends GPropertyReader {
    GComponent getReaderComponent();

    @Override
    default void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        update(controller.getComponentController(getReaderComponent()), values, partial);
    }
    // what kind of reader it is: one React draws nothing from - an element class, a value class, a custom design
    default void update(GComponentController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        controller.updateOther(this, values, partial);
    }
    // where the platform draws the component: the form's own update of it
    void updateLsf(GFormController form, NativeHashMap<GGroupObjectValue, PValue> values);
}
