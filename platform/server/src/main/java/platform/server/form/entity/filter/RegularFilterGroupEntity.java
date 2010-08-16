package platform.server.form.entity.filter;

import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupEntity {
    public final int ID;

    public RegularFilterGroupEntity(int iID) {
        ID = iID;
    }

    public List<RegularFilterEntity> filters = new ArrayList<RegularFilterEntity>();
    public void addFilter(RegularFilterEntity filter) {
        filters.add(filter);
    }

    public int defaultFilter = -1;
    public void addFilter(RegularFilterEntity filter, boolean setDefault) {
        if(setDefault)
            defaultFilter = filters.size();
        filters.add(filter);
    }
}
