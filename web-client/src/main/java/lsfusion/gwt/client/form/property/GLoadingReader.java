package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GLsfPropertyController;

// whether a change of a cell the client sent still waits for its answer: the client's own, never the server's - set
// when the change is sent, taken off by the answer to it (GFormController.modifyFormChangesWithLoadingPropertyAsyncs)
public class GLoadingReader extends GExtraAttributeReader {

    public GLoadingReader(){}

    public GLoadingReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "LOADING");
    }

    public void updateLsf(GLsfPropertyController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
        controller.updateLoadings(this, values);
    }

    @Override
    public String getAttributeField() { return "loading"; }
    @Override
    public GAttributeConverter getAttributeConverter() { return GAttributeConverter.FLAG; }
}
