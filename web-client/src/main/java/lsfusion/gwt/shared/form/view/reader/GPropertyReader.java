package lsfusion.gwt.form.shared.view.reader;

import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;

import java.io.Serializable;
import java.util.Map;

public interface GPropertyReader extends Serializable {
    void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys);
    int getGroupObjectID();
}
