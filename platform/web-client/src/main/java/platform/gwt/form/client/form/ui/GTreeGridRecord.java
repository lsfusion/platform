package platform.gwt.form.client.form.ui;

import platform.gwt.form.shared.view.GGroupObject;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.HashMap;
import java.util.Map;

public class GTreeGridRecord extends GridDataRecord {
    private GGroupObject group;

    public GTreeGridRecord(GGroupObject group, GGroupObjectValue key, HashMap<GPropertyDraw, Object> values) {
        super(-1, key);
        this.group = group;

        for (Map.Entry<GPropertyDraw, Object> e : values.entrySet()) {
            setAttribute(e.getKey().sID, e.getValue());
        }
    }

    public GGroupObject getGroup() {
        return group;
    }
}
