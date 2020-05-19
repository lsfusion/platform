package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.HashMap;
import java.util.Map;

public class GTreeGridRecord extends GridDataRecord {
    private GGroupObject group;

    public GTreeGridRecord(GGroupObject group, GGroupObjectValue key, HashMap<GPropertyDraw, Object> values) {
        super(key);
        this.group = group;

        for (Map.Entry<GPropertyDraw, Object> e : values.entrySet()) {
            setValue(e.getKey(), e.getValue());
        }
    }

    public void setTreeValue(Object value) {
        setAttribute("treeColumn", value);
    }
    public Object getTreeValue() {
        return getAttribute("treeColumn");
    }
    public void setValue(GPropertyDraw property, Object value) {
        setAttribute(property.ID, value);
    }
    public Object getValue(GPropertyDraw property) {
        return getAttribute(property.ID);
    }

    public GGroupObject getGroup() {
        return group;
    }
}
