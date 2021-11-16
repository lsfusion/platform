package lsfusion.gwt.client.form.object.table.tree;

import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.GToolbar;

import java.util.ArrayList;
import java.util.List;

public class GTreeGroup extends GComponent {
    public List<GGroupObject> groups = new ArrayList<>();

    public GContainer filtersContainer;
    public List<GFilter> filters = new ArrayList<>();

    public GToolbar toolbar;
    
    public boolean expandOnClick;

    public int headerHeight;

    public int calculateSize() {
        int size = 0;
        for (GGroupObject groupObject : groups) {
            size += groupObject.isRecursive ? 20 * 4 : 20;
        }
        return size;
    }
}
