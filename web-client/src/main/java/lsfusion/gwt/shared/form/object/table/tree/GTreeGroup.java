package lsfusion.gwt.shared.form.object.table.tree;

import lsfusion.gwt.shared.form.design.GComponent;
import lsfusion.gwt.shared.form.filter.user.GFilter;
import lsfusion.gwt.shared.form.object.GGroupObject;
import lsfusion.gwt.shared.form.object.table.GToolbar;

import java.util.ArrayList;
import java.util.List;

public class GTreeGroup extends GComponent {
    public List<GGroupObject> groups = new ArrayList<>();

    public GToolbar toolbar;
    public GFilter filter;
    
    public boolean expandOnClick;

    public int calculateSize() {
        int size = 0;
        for (GGroupObject groupObject : groups) {
            size += groupObject.isRecursive ? 20 * 4 : 20;
        }
        return size;
    }
}
