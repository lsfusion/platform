package lsfusion.gwt.client.form.object.table.tree;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.GToolbar;

import java.util.ArrayList;

public class GTreeGroup extends GComponent {
    public ArrayList<GGroupObject> groups = new ArrayList<>();

    public GContainer filtersContainer;
    public ArrayList<GFilter> filters = new ArrayList<>();

    public boolean autoSize;
    public Boolean boxed;

    public GToolbar toolbar;
    
    public boolean expandOnClick;

    public int headerHeight;

    public GSize getHeaderHeight() {
        if(headerHeight >= 0)
            return GSize.getValueSize(headerHeight);
        return null;
    }

    public GSize getExpandWidth() {
        GSize size = GSize.ZERO;
        for (GGroupObject groupObject : groups) {
            GSize groupSize = GSize.CONST(20);
            if(groupObject.isRecursive)
                groupSize = groupSize.scale(4);
            size = size.add(groupSize);
        }
        return size;
    }

    private GGroupObject getLastGroup() {
        return groups.get(groups.size() - 1);
    }

    public int lineWidth;
    public int lineHeight;

    @Override
    protected GSize getDefaultWidth() {
        return getLastGroup().getWidth(lineWidth).add(getExpandWidth());
    }

    @Override
    protected GSize getDefaultHeight() {
        return getLastGroup().getHeight(lineHeight, getHeaderHeight());
    }

    public GGroupObjectValue filterRowKeys(GGroupObject groupObject, GGroupObjectValue fullCurrentKey) {
        return fullCurrentKey.filter(groups.subList(0, groups.indexOf(groupObject) + 1));
    }
}
