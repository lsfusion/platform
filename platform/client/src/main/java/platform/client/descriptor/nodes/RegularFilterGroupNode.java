package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.RegularFilterGroupEditor;
import platform.client.descriptor.filter.RegularFilterDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public class RegularFilterGroupNode extends GroupElementNode<RegularFilterGroupDescriptor> {

    public RegularFilterGroupNode(GroupObjectDescriptor groupObject, RegularFilterGroupDescriptor regularFilterGroup) {
        super(groupObject, regularFilterGroup);

        for(RegularFilterDescriptor regularFilter : regularFilterGroup.filters)
            add(new RegularFilterNode(groupObject, regularFilter));
    }

    public JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new RegularFilterGroupEditor(groupObject, getDescriptor());
    }
}
