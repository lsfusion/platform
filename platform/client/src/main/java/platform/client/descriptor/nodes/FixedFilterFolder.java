package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;

import java.util.List;
import java.util.Set;

public class FixedFilterFolder extends GroupElementFolder<FixedFilterFolder> {

    private Set<FilterDescriptor> fixedFilters;

    public FixedFilterFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, Set<FilterDescriptor> fixedFilters) {
        super(group, "Постоянные фильтры");

        this.fixedFilters = fixedFilters;
        for (FilterDescriptor filter : fixedFilters)
            if(group==null || group.equals(filter.getGroupObject(groupList)))
                add(new FixedFilterNode(group, filter));
    }
}
