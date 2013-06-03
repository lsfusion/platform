package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.RegularFilterEditor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.filter.FilterDescriptor;
import lsfusion.client.descriptor.filter.RegularFilterDescriptor;
import lsfusion.client.descriptor.nodes.GroupElementNode;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;

public class RegularFilterNode extends GroupElementNode<RegularFilterDescriptor, RegularFilterNode>  implements EditableTreeNode {

    public RegularFilterNode(GroupObjectDescriptor group, RegularFilterDescriptor descriptor) {
        super(group, descriptor);

        addFieldReferenceNode(descriptor, "filter", ClientResourceBundle.getString("descriptor.filter"), group, FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new RegularFilterEditor(groupObject, getTypedObject(), form);
    }
}
