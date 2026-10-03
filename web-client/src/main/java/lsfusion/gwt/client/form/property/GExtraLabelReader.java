package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

// what labels a property - its caption, image, comment, footer: the property's, as all of it is, so it goes to the
// property's controller (GPropertyController.updateLabel): React's entry of it where React draws or labels the
// property, else the platform's, which hands it to the reader's own update of its view (updateLsf)
public abstract class GExtraLabelReader extends GExtraAttributeReader {
    public GExtraLabelReader() {
    }
    public GExtraLabelReader(int propertyID, int groupObjectID, String prefix) {
        super(propertyID, groupObjectID, prefix);
    }

    // what kind of reader it is: what labels the property
    @Override
    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        controller.updateLabel(this, values, partial);
    }
}
