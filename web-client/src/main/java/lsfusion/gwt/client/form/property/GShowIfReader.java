package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

public class GShowIfReader extends GExtraPropertyReader {

    public GShowIfReader(){}

    public GShowIfReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "SHOWIF");
    }

    public void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateShowIfValues(this, values);
    }

    // showIf is not an attribute OF an entry, it decides whether the entry EXISTS - which is why it has no attribute
    // field and no converter, and why the projection routes it before the attribute path rather than through it
    @Override
    public boolean isPresenceReader() { return true; }
}
