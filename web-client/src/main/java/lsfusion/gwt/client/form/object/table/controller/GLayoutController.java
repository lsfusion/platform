package lsfusion.gwt.client.form.object.table.controller;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GComponentLabelReader;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.PValue;

// THE PLATFORM'S CONTROLLER OF THE COMPONENTS IT DRAWS: one for the form - a component reader names its component
// itself, so there is nothing to bind. What a reader brings goes to its own update of the form (updateLsf): the layout.
// A property's class as a component comes here too (GReactFormData.createComponentController)
public class GLayoutController implements GComponentController {
    private final GFormController form;

    public GLayoutController(GFormController form) {
        this.form = form;
    }
    public void updateLabel(GComponentLabelReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        reader.updateLsf(form, values);
    }
    public void updateOther(GComponentReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        reader.updateLsf(form, values);
    }
}
