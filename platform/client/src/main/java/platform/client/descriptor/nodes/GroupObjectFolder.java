package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class GroupObjectFolder extends DefaultMutableTreeNode {

    public GroupObjectFolder(List<GroupObjectDescriptor> groups, List<PropertyDrawDescriptor> properties) {
        super("Группы объектов");

        for (GroupObjectDescriptor descriptor : groups) {
            add(new GroupObjectNode(descriptor, properties));
        }

    }
}
