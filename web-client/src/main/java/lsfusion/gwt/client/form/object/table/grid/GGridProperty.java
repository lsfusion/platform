package lsfusion.gwt.client.form.object.table.grid;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.PValue;

public class GGridProperty extends GComponent {

    public String valueClass;

    private class GValueElementClassReader implements GPropertyReader {
        private String sID;

        public GValueElementClassReader() {
        }

        @Override
        public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
            controller.getFormLayout().setValueClass(GGridProperty.this, PValue.getClassStringValue(values.get(GGroupObjectValue.EMPTY)));
        }

        @Override
        public String getNativeSID() {
            if(sID == null) {
                sID = "_GRID_" + "VALUEELEMENTCLASSREADER" + "_" + GGridProperty.this.sID;
            }
            return sID;
        }
    }
    public final GPropertyReader valueElementClassReader = new GValueElementClassReader();
}
