package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

public class GMapTileProviderReader extends GRowPropertyReader {
    public GMapTileProviderReader() {
    }

    public GMapTileProviderReader(int readerID) {
        super(readerID, "MAP_TILE_PROVIDER");
    }

    @Override
    protected void update(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        controller.updateMapTileProviderValues(values);
    }
}
