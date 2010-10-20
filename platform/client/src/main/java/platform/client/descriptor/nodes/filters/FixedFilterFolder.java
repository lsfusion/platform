package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.nodes.GroupElementFolder;
import platform.client.descriptor.nodes.actions.NewElementListener;

import java.util.List;
import java.util.Set;

public class FixedFilterFolder extends GroupElementFolder<FixedFilterFolder> implements NewElementListener<FilterDescriptor> {

    private Set<FilterDescriptor> fixedFilters;

    public FixedFilterFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, Set<FilterDescriptor> fixedFilters) {
        super(group, "Постоянные фильтры");

        this.fixedFilters = fixedFilters;
        for (FilterDescriptor filter : fixedFilters)
            if(group==null || group.equals(filter.getGroupObject(groupList)))
                add(filter.getNode(group));

        FilterDescriptor.addNewElementActions(this, this);
    }

    public void newElement(FilterDescriptor element) {
        fixedFilters.add(element);
        IncrementDependency.update(this, "fixedFilters");
    }
}
