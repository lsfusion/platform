package platform.client.descriptor.nodes;

import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.base.BaseUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Set;
import java.util.List;

public class FixedFilterFolder extends GroupElementFolder {

    Set<FilterDescriptor> fixedFilters;
    public FixedFilterFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, Set<FilterDescriptor> fixedFilters) {
        super(group, "Постоянные фильтры");
        
        this.fixedFilters = fixedFilters;
        for (FilterDescriptor filter : fixedFilters)
            if(group==null || group.equals(filter.getGroupObject(groupList)))
                add(new FixedFilterNode(group, filter));
    }
}
