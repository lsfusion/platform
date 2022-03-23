package lsfusion.gwt.client.form.object.table.tree.view;

import com.google.gwt.core.client.GWT;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GTreeGridRecord extends GridDataRecord {
    private GGroupObject group;

    public GTreeGridRecord(GGroupObject group, GGroupObjectValue key, HashMap<GPropertyDraw, Pair<Object, Boolean>> values) {
        super(key);
        this.group = group;

        for (Map.Entry<GPropertyDraw, Pair<Object, Boolean>> e : values.entrySet()) {
            GPropertyDraw vKey = e.getKey();
            Pair<Object, Boolean> value = e.getValue();
            setValue(vKey, value.first);
            setLoading(vKey, value.second);
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
    public void setLoading(GPropertyDraw property, boolean loading) {
        setAttribute(property.sID + "_loading", loading ? true : null);
    }
    public boolean isLoading(GPropertyDraw property) {
        return getAttribute(property.sID + "_loading") != null;
    }
    public void setImage(GPropertyDraw property, Object image) {
        setAttribute(property.sID + "_image", image);
    }
    public Object getImage(GPropertyDraw property) {
        return getAttribute(property.sID + "_image");
    }

    public GGroupObject getGroup() {
        return group;
    }
}
