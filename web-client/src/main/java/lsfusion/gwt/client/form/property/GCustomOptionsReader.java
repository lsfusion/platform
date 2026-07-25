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
    public String getAttributeField() { return "options"; }
    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.JSON; }
    @Override
    public GGroupAttributeScope getAttributeScope() { return GGroupAttributeScope.GROUP; } // group-scoped: one value at EMPTY -> node.options
}
