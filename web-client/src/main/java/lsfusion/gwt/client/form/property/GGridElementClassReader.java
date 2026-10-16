package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPlatformPropertyController;

public class GGridElementClassReader extends GExtraPropertyReader {

    public GGridElementClassReader() {
    }

    public GGridElementClassReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "GRIDELEMENTCLASS");
    }

    public void updatePlatform(GPlatformPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> keys) {
        controller.updateCellGridElementClasses(this, keys);
    }

    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.CLASS; }
}