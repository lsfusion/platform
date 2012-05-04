package platform.client.descriptor.nodes.filters;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.RegularFilterEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.filter.RegularFilterDescriptor;
import platform.client.descriptor.nodes.GroupElementNode;
import platform.client.descriptor.nodes.actions.EditableTreeNode;

public class RegularFilterNode extends GroupElementNode<RegularFilterDescriptor, RegularFilterNode>  implements EditableTreeNode {

    public RegularFilterNode(GroupObjectDescriptor group, RegularFilterDescriptor descriptor) {
        super(group, descriptor);

        addFieldReferenceNode(descriptor, "filter", ClientResourceBundle.getString("descriptor.filter"), group, FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new RegularFilterEditor(groupObject, getTypedObject(), form);
    }
}
