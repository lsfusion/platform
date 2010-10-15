package platform.client.descriptor.nodes;

import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.FixedFilterEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

public class FixedFilterNode extends GroupElementNode<FilterDescriptor, FixedFilterNode> {

    public FixedFilterNode(GroupObjectDescriptor group, FilterDescriptor userObject) {
        super(group, userObject);
    }

    public NodeEditor createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new FixedFilterEditor(groupObject, getTypedObject());
    }
}
