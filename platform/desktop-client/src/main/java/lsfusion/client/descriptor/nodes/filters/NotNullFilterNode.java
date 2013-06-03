package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.editor.filters.NotNullFilterEditor;
import lsfusion.client.descriptor.filter.NotNullFilterDescriptor;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;

public class NotNullFilterNode extends PropertyFilterNode<NotNullFilterDescriptor, NotNullFilterNode> implements EditableTreeNode {

    public NotNullFilterNode(GroupObjectDescriptor group, NotNullFilterDescriptor descriptor) {
        super(group, descriptor);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new NotNullFilterEditor(groupObject, getTypedObject(), form);
    }
}
