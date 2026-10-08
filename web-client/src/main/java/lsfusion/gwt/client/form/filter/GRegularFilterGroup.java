package lsfusion.gwt.client.form.filter;

import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObject;

import java.util.ArrayList;

public class GRegularFilterGroup extends GComponent {
    public ArrayList<GRegularFilter> filters = new ArrayList<>();
    public GGroupObject groupObject;
    public boolean noNull;

    // null for none
    public GRegularFilter getFilter(int filterID) {
        for (GRegularFilter filter : filters)
            if (filter.ID == filterID)
                return filter;
        return null;
    }
}
