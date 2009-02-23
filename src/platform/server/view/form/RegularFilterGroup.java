package platform.server.view.form;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class RegularFilterGroup implements Serializable {

    public int ID;
    public RegularFilterGroup(int iID) {
        ID = iID;
    }

    public List<RegularFilter> filters = new ArrayList<RegularFilter>();
    public void addFilter(RegularFilter filter) {
        filters.add(filter);
    }

    public RegularFilter getFilter(int filterID) {
        for (RegularFilter filter : filters)
            if (filter.ID == filterID)
                return filter;
        return null;
    }
}
