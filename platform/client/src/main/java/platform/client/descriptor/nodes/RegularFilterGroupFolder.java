package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;

import java.util.List;

public class RegularFilterGroupFolder extends GroupElementFolder {

    private GroupObjectDescriptor group;

    public RegularFilterGroupFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, List<RegularFilterGroupDescriptor> regularFilters) {
        super(group, null);

        this.group = group;

        setUserObject(this);

        for (RegularFilterGroupDescriptor filter : regularFilters)
            if(group==null || group.equals(filter.getGroupObject(groupList)))
                add(new RegularFilterGroupNode(group, filter));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegularFilterGroupFolder that = (RegularFilterGroupFolder) o;

        if (group != null ? !group.equals(that.group) : that.group != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return group != null ? group.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Стандартные фильтры";
    }
}
