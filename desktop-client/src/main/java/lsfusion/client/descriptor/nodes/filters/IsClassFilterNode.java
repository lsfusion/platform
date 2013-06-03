package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.editor.filters.IsClassFilterEditor;
import lsfusion.client.descriptor.filter.IsClassFilterDescriptor;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;

public class IsClassFilterNode extends PropertyFilterNode<IsClassFilterDescriptor, IsClassFilterNode> implements EditableTreeNode {

    public IsClassFilterNode(GroupObjectDescriptor group, IsClassFilterDescriptor descriptor) {
        super(group, descriptor);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new IsClassFilterEditor(groupObject, getTypedObject(), form);
    }
}
