package platform.client.descriptor.nodes;

import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.base.BaseUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Set;
import java.util.List;

public class FixedFilterFolder extends GroupElementFolder {

    private GroupObjectDescriptor group;
    private Set<FilterDescriptor> fixedFilters;

    public FixedFilterFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, Set<FilterDescriptor> fixedFilters) {
        super(group, null);

        this.group = group;

        setUserObject(group);

        this.fixedFilters = fixedFilters;
        for (FilterDescriptor filter : fixedFilters)
            if(group==null || group.equals(filter.getGroupObject(groupList)))
                add(new FixedFilterNode(group, filter));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FixedFilterFolder that = (FixedFilterFolder) o;

        if (group != null ? !group.equals(that.group) : that.group != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return group != null ? group.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Постоянные фильтры";
    }
}
