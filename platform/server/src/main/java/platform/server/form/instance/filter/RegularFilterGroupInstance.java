package platform.server.form.instance.filter;

import platform.server.form.instance.GroupObjectInstance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupInstance implements Serializable {

    public int ID;
    public RegularFilterGroupInstance(int iID) {
        ID = iID;
    }

    public List<RegularFilterInstance> filters = new ArrayList<RegularFilterInstance>();
    public void addFilter(RegularFilterInstance filter) {
        filters.add(filter);
    }

    public RegularFilterInstance getFilter(int filterID) {
        for (RegularFilterInstance filter : filters)
            if (filter.ID == filterID)
                return filter;
        return null;
    }
    
    public GroupObjectInstance getApplyObject() {
        return filters.iterator().next().filter.getApplyObject();
    }
}
