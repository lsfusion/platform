package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.HasNativeSID;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

import java.io.Serializable;

public interface GPropertyReader extends Serializable, HasNativeSID {
    void update(GFormController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys);
}
