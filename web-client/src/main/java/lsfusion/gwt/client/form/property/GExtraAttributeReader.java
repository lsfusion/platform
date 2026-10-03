package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

// a property's reader that is an ATTRIBUTE of it - of its cell or of its column: it names the field it is projected
// into
public abstract class GExtraAttributeReader extends GExtraPropertyReader {
    public GExtraAttributeReader() {
    }
    public GExtraAttributeReader(int propertyID, int groupObjectID, String prefix) {
        super(propertyID, groupObjectID, prefix);
    }

    @Override
    public abstract String getAttributeField();

    @Override
    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        controller.updateAttribute(this, values, partial);
    }
}
