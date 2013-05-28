package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.filters.NotNullFilterEditor;
import platform.client.descriptor.filter.NotNullFilterDescriptor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;

public class NotNullFilterNode extends PropertyFilterNode<NotNullFilterDescriptor, NotNullFilterNode> implements EditableTreeNode {

    public NotNullFilterNode(GroupObjectDescriptor group, NotNullFilterDescriptor descriptor) {
        super(group, descriptor);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new NotNullFilterEditor(groupObject, getTypedObject(), form);
    }
}
