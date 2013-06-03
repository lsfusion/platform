package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.editor.filters.CompareFilterEditor;
import lsfusion.client.descriptor.filter.CompareFilterDescriptor;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;
import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ApplicationContextProvider;

public class CompareFilterNode extends PropertyFilterNode<CompareFilterDescriptor, CompareFilterNode> implements EditableTreeNode, ApplicationContextProvider {

    public CompareFilterNode(GroupObjectDescriptor group, CompareFilterDescriptor descriptor) {
        super(group, descriptor);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new CompareFilterEditor(groupObject, getTypedObject(), form);
    }

    public ApplicationContext getContext() {
        return getTypedObject().getContext();
    }
}
