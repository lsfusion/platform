package platform.server.view.navigator;

import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupNavigator {
    public final int ID;

    public RegularFilterGroupNavigator(int iID) {
        ID = iID;
    }

    public List<RegularFilterNavigator> filters = new ArrayList<RegularFilterNavigator>();
    public void addFilter(RegularFilterNavigator filter) {
        filters.add(filter);
    }

    public int defaultFilter = -1;
    public void addFilter(RegularFilterNavigator filter, boolean setDefault) {
        if(setDefault)
            defaultFilter = filters.size();
        filters.add(filter);
    }
}
