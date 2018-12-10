package lsfusion.gwt.shared.form.view.reader;

import lsfusion.gwt.shared.changes.GGroupObjectValue;
import lsfusion.gwt.shared.form.view.logics.GGroupObjectLogicsSupplier;

import java.io.Serializable;
import java.util.Map;

public interface GPropertyReader extends Serializable {
    void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys);
    int getGroupObjectID();
}
