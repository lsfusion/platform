package platform.client.descriptor.nodes;

import platform.client.descriptor.filter.RegularFilterDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.RegularFilterEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public class RegularFilterNode extends GroupElementNode<RegularFilterDescriptor> {

    public RegularFilterNode(GroupObjectDescriptor groupObject, RegularFilterDescriptor userObject) {
        super(groupObject, userObject);
    }

    public JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new RegularFilterEditor(groupObject, getDescriptor());
    }
}
