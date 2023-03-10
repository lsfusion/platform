package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class GTreeGridRecord extends GridDataRecord {
    private GGroupObject group;

    public GTreeGridRecord(int rowIndex, GTreeContainerTableNode node, GTreeColumnValue treeValue) {
        super(rowIndex);

        setKey(node.getKey());
        setTreeValue(treeValue);

        this.group = node.getGroup();
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
    public void setValueElementClass(GPropertyDraw property, Object valueElementClass) {
        setAttribute(property.sID + "_valueelementclass", valueElementClass != null ? valueElementClass.toString() : null);
    }
    public String getValueElementClass(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_valueelementclass");
    }
    public void setBackground(GPropertyDraw property, Object background) {
        setAttribute(property.sID + "_background", background != null ? background.toString() : null);
    }
    public String getBackground(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_background");
    }
    public void setForeground(GPropertyDraw property, Object foreground) {
        setAttribute(property.sID + "_foreground", foreground != null ? foreground.toString() : null);
    }
    public String getForeground(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_foreground");
    }

    public GGroupObject getGroup() {
        return group;
    }
}
