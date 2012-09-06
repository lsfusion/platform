package platform.gwt.form2.shared.view;

import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

import java.util.HashMap;

public class GTreeGridRecord extends GridDataRecord {
    private GGroupObject group;

    public GTreeGridRecord(GGroupObject group, GGroupObjectValue key, HashMap<GPropertyDraw, Object> values) {
        super(key, values);
        this.group = group;
    }

    public GGroupObject getGroup() {
        return group;
    }
}
