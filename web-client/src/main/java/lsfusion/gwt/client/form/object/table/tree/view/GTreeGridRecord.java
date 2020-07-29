package lsfusion.gwt.client.form.object.table.tree.view;

import com.google.gwt.core.client.GWT;
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

    private final String treeColumn = "treeColumn";
    public void setTreeValue(GTreeColumnValue value) {
        setAttribute(treeColumn, value);
    }
    public GTreeColumnValue getTreeValue() {
        return (GTreeColumnValue) getAttribute(treeColumn);
    }
    public void setValue(GPropertyDraw property, Object value) {
        setAttribute(property.sID, value);
    }
    public Object getValue(GPropertyDraw property) {
        return getAttribute(property.sID);
    }

    public GGroupObject getGroup() {
        return group;
    }
}
