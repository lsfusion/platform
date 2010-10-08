package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.RegularFilterDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;
import platform.base.BaseUtils;

import java.util.Set;
import java.util.List;

public class RegularFilterGroupFolder extends GroupElementFolder {

    public RegularFilterGroupFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor groupObject, List<RegularFilterGroupDescriptor> regularFilters) {
        super(groupObject, "Стандартные фильтры");
        
        for (RegularFilterGroupDescriptor filter : regularFilters)
            if(groupObject==null || groupObject.equals(filter.getGroupObject(groupList)))
                add(new RegularFilterGroupNode(groupObject, filter));
    }
}
