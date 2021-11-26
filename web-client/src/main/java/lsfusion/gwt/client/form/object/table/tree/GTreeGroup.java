package lsfusion.gwt.client.form.object.table.tree;

import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTableHeader;

import java.util.ArrayList;
import java.util.List;

public class GTreeGroup extends GComponent {
    public List<GGroupObject> groups = new ArrayList<>();

    public GContainer filtersContainer;
    public List<GFilter> filters = new ArrayList<>();

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
        return getLastGroup().getHeight(lineHeight) + (headerHeight >= 0 ? headerHeight : GGridPropertyTableHeader.DEFAULT_HEADER_HEIGHT);
    }
}
