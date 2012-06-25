package platform.gwt.view.reader;

import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.logics.GGroupObjectLogicsSupplier;

import java.io.Serializable;
import java.util.Map;

public interface GPropertyReader extends Serializable {
    void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values);
    int getGroupObjectID();
}
