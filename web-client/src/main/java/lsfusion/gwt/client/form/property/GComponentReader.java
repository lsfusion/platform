package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;

// a reader of a COMPONENT's own attribute - its showIf, its element class, a container's caption, a grid's value class
// ...: the component it is of, and its SID - the component's, after a prefix of the reader's kind, built at first use,
// as the component's own SID is set after its readers are made
public abstract class GComponentReader implements GPropertyReader {
    private final GComponent component;
    private final String sidPrefix;
    private String sID;

    protected GComponentReader(GComponent component, String sidPrefix) {
        this.component = component;
        this.sidPrefix = sidPrefix;
    }

    public GComponent getReaderComponent() {
        return component;
    }

    @Override
    public String getNativeSID() {
        if (sID == null)
            sID = sidPrefix + "_" + component.sID;
        return sID;
    }

    @Override
    public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        update(controller.getComponentController(component), values, partial);
    }
    // what kind of reader it is: one React draws nothing from - an element class, a value class, a custom design
    public void update(GComponentController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        controller.updateOther(this, values, partial);
    }
    // where the platform draws the component: the form's own update of it
    public abstract void updatePlatform(GFormController form, NativeHashMap<GGroupObjectValue, PValue> values);
}
