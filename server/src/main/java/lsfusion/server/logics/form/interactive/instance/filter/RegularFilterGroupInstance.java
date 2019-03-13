package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupInstance implements Serializable {

    public RegularFilterGroupEntity entity;

    public int getID() {
        return entity.ID;
    }

    public RegularFilterGroupInstance(RegularFilterGroupEntity entity) {
        this.entity = entity;
    }

    public List<RegularFilterInstance> filters = new ArrayList<>();
    public void addFilter(RegularFilterInstance filter) {
        filters.add(filter);
    }

    public RegularFilterInstance getFilter(int filterID) {
        for (RegularFilterInstance filter : filters)
            if (filter.getID() == filterID)
                return filter;
        return null;
    }
    
    public GroupObjectInstance getApplyObject() {
        return filters.iterator().next().filter.getApplyObject();
    }
}
