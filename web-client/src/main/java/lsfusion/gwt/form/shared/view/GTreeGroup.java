package lsfusion.gwt.form.shared.view;

import lsfusion.client.logics.ClientGroupObject;

import java.util.ArrayList;
import java.util.List;

public class GTreeGroup extends GComponent {
    public List<GGroupObject> groups = new ArrayList<>();

    public GToolbar toolbar;
    public GFilter filter;
    
    public boolean expandOnClick;

    public int calculatePreferredSize() {
        int size = 0;
        for (GGroupObject groupObject : groups) {
            size += groupObject.isRecursive ? 35 * 4 : 35;
        }
        return size;
    }
}
