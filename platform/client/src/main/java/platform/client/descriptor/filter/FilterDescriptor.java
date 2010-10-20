package platform.client.descriptor.filter;

import platform.client.ClientTreeNode;
import platform.client.descriptor.nodes.actions.NewElementListener;
import platform.client.descriptor.nodes.filters.FilterNode;
import platform.client.serialization.ClientCustomSerializable;
import platform.client.descriptor.GroupObjectDescriptor;

import java.util.List;

public abstract class FilterDescriptor implements ClientCustomSerializable {

    public abstract GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList);

    public static GroupObjectDescriptor getDownGroup(GroupObjectDescriptor group1, GroupObjectDescriptor group2, List<GroupObjectDescriptor> groupList) {
        if(groupList.indexOf(group1)>groupList.indexOf(group2))
            return group1;
        else
            return group2;
    }

    public abstract FilterNode getNode(GroupObjectDescriptor group);

    public static void addNewElementActions(ClientTreeNode node, final NewElementListener listener) {
        node.addNewElementActions(new String[] {"Сравнение", "Определено", "Класс", "Отрицание", "Или"},
                new Class[] {CompareFilterDescriptor.class, NotNullFilterDescriptor.class, IsClassFilterDescriptor.class, NotFilterDescriptor.class, OrFilterDescriptor.class},
                listener);
    }
}
