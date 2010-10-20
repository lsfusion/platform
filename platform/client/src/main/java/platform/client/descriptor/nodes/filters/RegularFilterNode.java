package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.filter.RegularFilterDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.RegularFilterEditor;
import platform.client.descriptor.nodes.GroupElementNode;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.interop.serialization.RemoteDescriptorInterface;

public class RegularFilterNode extends GroupElementNode<RegularFilterDescriptor, RegularFilterNode>  implements EditableTreeNode {

    public RegularFilterNode(GroupObjectDescriptor group, RegularFilterDescriptor descriptor) {
        super(group, descriptor);

        add(descriptor.filter.createNode(group));
    }

    public NodeEditor createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new RegularFilterEditor(groupObject, getTypedObject());
    }
}
