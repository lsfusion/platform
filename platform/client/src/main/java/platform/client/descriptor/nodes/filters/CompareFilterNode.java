package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.filters.CompareFilterEditor;
import platform.client.descriptor.filter.CompareFilterDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;

public class CompareFilterNode extends PropertyFilterNode<CompareFilterDescriptor, CompareFilterNode> implements EditableTreeNode {

    public CompareFilterNode(GroupObjectDescriptor group, CompareFilterDescriptor descriptor) {
        super(group, descriptor);

        addCollectionReferenceActions(this, "fixedFilters", FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new CompareFilterEditor(groupObject, getTypedObject(), form);
    }
}
