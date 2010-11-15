package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.filters.CompareFilterEditor;
import platform.client.descriptor.filter.CompareFilterDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.interop.context.ApplicationContext;
import platform.interop.context.ApplicationContextProvider;

public class CompareFilterNode extends PropertyFilterNode<CompareFilterDescriptor, CompareFilterNode> implements EditableTreeNode, ApplicationContextProvider {

    public CompareFilterNode(GroupObjectDescriptor group, CompareFilterDescriptor descriptor) {
        super(group, descriptor);

        addCollectionReferenceActions(this, "fixedFilters", FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new CompareFilterEditor(groupObject, getTypedObject(), form);
    }

    public ApplicationContext getContext() {
        return getTypedObject().getContext();
    }
}
