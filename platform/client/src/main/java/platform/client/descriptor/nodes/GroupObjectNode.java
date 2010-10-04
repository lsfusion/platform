package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class GroupObjectNode extends DefaultMutableTreeNode {

    public GroupObjectNode(GroupObjectDescriptor group, List<PropertyDrawDescriptor> propertyDraws) {
        super(group);

        add(new ObjectFolder(group));
        add(new PropertyDrawFolder(group, propertyDraws));
    }
}
