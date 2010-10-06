package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.editor.GroupObjectEditor;

import javax.swing.*;
import java.util.List;

public class GroupObjectNode extends DescriptorNode<GroupObjectDescriptor> implements EditingTreeNode {

    public GroupObjectNode(GroupObjectDescriptor group, List<PropertyDrawDescriptor> propertyDraws) {
        super(group);

        add(new ObjectFolder(group));
        add(new PropertyDrawFolder(group, propertyDraws));
    }

    public JComponent createEditor() {
        return new GroupObjectEditor(getDescriptor());
    }
}
