package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.view.GridDataRecord;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

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
    public void setValue(GPropertyDraw property, PValue value) {
        setAttribute(property.sID, value);
    }
    public PValue getValue(GPropertyDraw property) {
        return (PValue) getAttribute(property.sID);
    }
    public void setLoading(GPropertyDraw property, boolean loading) {
        setAttribute(property.sID + "_loading", loading ? true : null);
    }
    public boolean isLoading(GPropertyDraw property) {
        return getAttribute(property.sID + "_loading") != null;
    }
    public void setImage(GPropertyDraw property, AppBaseImage image) {
        setAttribute(property.sID + "_image", image);
    }
    public AppBaseImage getImage(GPropertyDraw property) {
        return (AppBaseImage) getAttribute(property.sID + "_image");
    }
    public void setValueElementClass(GPropertyDraw property, String valueElementClass) {
        setAttribute(property.sID + "_valueelementclass", valueElementClass);
    }
    public String getValueElementClass(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_valueelementclass");
    }
    public void setBackground(GPropertyDraw property, String background) {
        setAttribute(property.sID + "_background", background);
    }
    public String getBackground(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_background");
    }
    public void setPlaceholder(GPropertyDraw property, String placeholder) {
        setAttribute(property.sID + "_placeholder", placeholder);
    }
    public String getPlaceholder(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_placeholder");
    }
    public void setForeground(GPropertyDraw property, String foreground) {
        setAttribute(property.sID + "_foreground", foreground);
    }
    public String getForeground(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_foreground");
    }

    public GGroupObject getGroup() {
        return group;
    }
}
