package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.RegularFilterGroupEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.filter.RegularFilterDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;
import platform.client.descriptor.nodes.GroupElementNode;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.tree.ClientTree;

import javax.swing.*;

public class RegularFilterGroupNode extends GroupElementNode<RegularFilterGroupDescriptor, RegularFilterGroupNode>  implements EditableTreeNode {

    private RegularFilterGroupDescriptor filterGroup;

    public RegularFilterGroupNode(GroupObjectDescriptor groupObject, RegularFilterGroupDescriptor filterGroup) {
        super(groupObject, filterGroup);

        this.filterGroup = filterGroup;

        for(RegularFilterDescriptor regularFilter : filterGroup.filters)
            add(new RegularFilterNode(groupObject, regularFilter));

        addCollectionReferenceActions(filterGroup, "filters", new String[] {""}, new Class[] {RegularFilterDescriptor.class});
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new RegularFilterGroupEditor(groupObject, getTypedObject());
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return ClientTree.getNode(info) instanceof RegularFilterNode;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return filterGroup.moveFilter(((RegularFilterNode) ClientTree.getNode(info)).getTypedObject(), ClientTree.getChildIndex(info));
    }
}
