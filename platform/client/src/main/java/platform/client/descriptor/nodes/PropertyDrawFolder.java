package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.base.BaseUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class PropertyDrawFolder extends GroupElementFolder {

    public PropertyDrawFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, List<PropertyDrawDescriptor> propertyDraws) {
        super(group, "Свойства");

        for (PropertyDrawDescriptor propertyDraw : propertyDraws)
            if (group==null || group.equals(propertyDraw.getGroupObject(groupList)))
                add(new PropertyDrawNode(group, propertyDraw));
    }
}
