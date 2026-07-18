package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;

public class GCustomOptionsReader extends GGroupObjectPropertyReader {

    public GCustomOptionsReader(){}

    public GCustomOptionsReader(int readerID) {
        super(readerID, "CUSTOM_OPTIONS");
    }
    public void update(GAbstractTableController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        controller.updateCustomOptionsValues(values);
    }

    @Override
    public String getMetaField() { return "customOptions"; }
    @Override
    public GMetaConverter getMetaConverter() { return GMetaConverter.JSON; }
    @Override
    public GMetaScope getMetaScope() { return GMetaScope.NODE; } // group-scoped: one value at EMPTY -> node.meta.customOptions
}
