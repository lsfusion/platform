package lsfusion.gwt.client.form.filter;

import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.object.GGroupObject;

import java.util.ArrayList;

public class GRegularFilterGroup extends GComponent {
    // its name in the form - FILTERGROUP fg's fg -, which a react view's part of it is named by on its group's node
    // (GReactFormData.FilterGroupPart)
    public String name;
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
