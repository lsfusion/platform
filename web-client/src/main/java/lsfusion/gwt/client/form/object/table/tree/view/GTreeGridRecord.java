package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.base.AppBaseImage;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.GInputBindingEvent;
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
    public void setGridElementClass(GPropertyDraw property, String gridElementClass) {
        setAttribute(property.sID + "_gridelementclass", gridElementClass);
    }
    public String getGridElementClass(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_gridelementclass");
    }
    public void setValueElementClass(GPropertyDraw property, String valueElementClass) {
        setAttribute(property.sID + "_valueelementclass", valueElementClass);
    }
    public String getValueElementClass(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_valueelementclass");
    }
    public void setFont(GPropertyDraw property, GFont font) {
        setAttribute(property.sID + "_font", font);
    }
    public GFont getFont(GPropertyDraw property) {
        return (GFont) getAttribute(property.sID + "_font");
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
    public void setPattern(GPropertyDraw property, String pattern) {
        setAttribute(property.sID + "_pattern", pattern);
    }
    public String getPattern(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_pattern");
    }
    public void setRegexp(GPropertyDraw property, String regexp) {
        setAttribute(property.sID + "_regexp", regexp);
    }
    public String getRegexp(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_regexp");
    }
    public void setRegexpMessage(GPropertyDraw property, String regexpMessage) {
        setAttribute(property.sID + "_regexpmessage", regexpMessage);
    }
    public String getRegexpMessage(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_regexpmessage");
    }
    public void setValueTooltip(GPropertyDraw property, String valueTooltip) {
        setAttribute(property.sID + "_valueTooltip", valueTooltip);
    }
    public String getValueTooltip(GPropertyDraw property) {
        return (String) getAttribute(property.sID + "_valueTooltip");
    }
    public void setPropertyCustomOptions(GPropertyDraw property, PValue propertyCustomOptions) {
        setAttribute(property.sID + "_propertyCustomOptions", propertyCustomOptions);
    }
    public PValue getPropertyCustomOptions(GPropertyDraw property) {
        return (PValue) getAttribute(property.sID + "_propertyCustomOptions");
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
