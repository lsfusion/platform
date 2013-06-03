package lsfusion.gwt.form.client.form.ui;

import lsfusion.gwt.form.shared.view.GGroupObject;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.HashMap;
import java.util.Map;

public class GTreeGridRecord extends GridDataRecord {
    private GGroupObject group;

    public GTreeGridRecord(GGroupObject group, GGroupObjectValue key, HashMap<GPropertyDraw, Object> values) {
        super(key);
        this.group = group;

        for (Map.Entry<GPropertyDraw, Object> e : values.entrySet()) {
            setAttribute(e.getKey().sID, e.getValue());
        }
    }

    public GGroupObject getGroup() {
        return group;
    }
}
