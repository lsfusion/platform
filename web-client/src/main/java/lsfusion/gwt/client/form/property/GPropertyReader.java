package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.HasSID;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;

import java.io.Serializable;

public interface GPropertyReader extends Serializable, HasSID {
    void update(GPropertyController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys);
    int getGroupObjectID();
}
