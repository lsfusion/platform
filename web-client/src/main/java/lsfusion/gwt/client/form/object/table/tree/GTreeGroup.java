package lsfusion.gwt.client.form.object.table.tree;

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

    public int getExpandWidth() {
        int size = 0;
        for (GGroupObject groupObject : groups) {
            size += groupObject.isRecursive ? 20 * 4 : 20;
        }
        return size;
    }

    private GGroupObject getLastGroup() {
        return groups.get(groups.size() - 1);
    }

    public int lineWidth;
    public int lineHeight;

    @Override
    protected Integer getDefaultWidth() {
        return getExpandWidth() + getLastGroup().getWidth(lineWidth);
    }

    @Override
    protected Integer getDefaultHeight() {
        return getLastGroup().getHeight(lineHeight, headerHeight);
    }

    public GGroupObjectValue filterRowKeys(GGroupObject groupObject, GGroupObjectValue fullCurrentKey) {
        return fullCurrentKey.filterIncl(groups.subList(0, groups.indexOf(groupObject) + 1));
    }
}
