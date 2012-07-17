package platform.gwt.view2.reader;

import platform.gwt.view2.changes.GGroupObjectValue;
import platform.gwt.view2.logics.GGroupObjectLogicsSupplier;

import java.io.Serializable;
import java.util.Map;

public interface GPropertyReader extends Serializable {
    void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values);
    int getGroupObjectID();
}
