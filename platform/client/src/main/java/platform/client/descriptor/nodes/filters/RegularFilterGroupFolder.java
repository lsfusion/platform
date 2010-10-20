package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;
import platform.client.descriptor.nodes.GroupElementFolder;

import java.util.List;

public class RegularFilterGroupFolder extends GroupElementFolder<RegularFilterGroupFolder> {

    public RegularFilterGroupFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, List<RegularFilterGroupDescriptor> regularFilters) {
        super(group, "Стандартные фильтры");

        for (RegularFilterGroupDescriptor filter : regularFilters)
            if(group==null || group.equals(filter.getGroupObject(groupList)))
                add(new RegularFilterGroupNode(group, filter));
    }
}
