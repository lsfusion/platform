package platform.client.descriptor.nodes;

import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.filter.RegularFilterDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.RegularFilterEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

public class RegularFilterNode extends GroupElementNode<RegularFilterDescriptor, RegularFilterNode> {

    public RegularFilterNode(GroupObjectDescriptor groupObject, RegularFilterDescriptor userObject) {
        super(groupObject, userObject);
    }

    public NodeEditor createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new RegularFilterEditor(groupObject, getTypedObject());
    }
}
