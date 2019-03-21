package lsfusion.gwt.shared.form.property;

import lsfusion.gwt.shared.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

import java.io.Serializable;
import java.util.Map;

public interface GPropertyReader extends Serializable {
    void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys);
    int getGroupObjectID();
}
