package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.RegularFilterGroupEditor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.filter.RegularFilterDescriptor;
import lsfusion.client.descriptor.filter.RegularFilterGroupDescriptor;
import lsfusion.client.descriptor.nodes.GroupElementNode;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;
import lsfusion.client.tree.ClientTree;

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
