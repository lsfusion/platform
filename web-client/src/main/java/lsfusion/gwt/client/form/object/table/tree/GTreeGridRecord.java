package lsfusion.gwt.client.form.object.table.tree;

import lsfusion.gwt.client.form.object.table.GridDataRecord;
import lsfusion.gwt.shared.form.object.GGroupObject;
import lsfusion.gwt.shared.form.property.GPropertyDraw;
import lsfusion.gwt.shared.form.object.GGroupObjectValue;

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
