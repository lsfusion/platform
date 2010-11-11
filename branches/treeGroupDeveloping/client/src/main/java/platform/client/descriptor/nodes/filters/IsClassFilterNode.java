package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.filters.IsClassFilterEditor;
import platform.client.descriptor.filter.IsClassFilterDescriptor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;

public class IsClassFilterNode extends PropertyFilterNode<IsClassFilterDescriptor, IsClassFilterNode> implements EditableTreeNode {

    public IsClassFilterNode(GroupObjectDescriptor group, IsClassFilterDescriptor descriptor) {
        super(group, descriptor);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new IsClassFilterEditor(groupObject, getTypedObject(), form);
    }
}
