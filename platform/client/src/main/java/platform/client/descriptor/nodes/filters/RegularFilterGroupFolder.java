package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;
import platform.client.descriptor.nodes.GroupElementFolder;

import java.util.List;

public class RegularFilterGroupFolder extends GroupElementFolder<RegularFilterGroupFolder> {

    public RegularFilterGroupFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, FormDescriptor form) {
        super(group, "Стандартные фильтры");

        for (RegularFilterGroupDescriptor filter : form.regularFilterGroups)
            if(group==null || group.equals(filter.getGroupObject(groupList)))
                add(new RegularFilterGroupNode(group, filter));

        addCollectionReferenceActions(form, "regularFilterGroups", new String[] {""}, new Class[] {RegularFilterGroupDescriptor.class});
    }
}
