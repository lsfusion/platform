package lsfusion.client.descriptor.nodes;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.TreeGroupDescriptor;
import lsfusion.client.descriptor.editor.TreeGroupEditor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;
import lsfusion.client.tree.ClientTree;

import javax.swing.*;

public class TreeGroupNode extends DescriptorNode<TreeGroupDescriptor, TreeGroupNode> implements EditableTreeNode {
    private FormDescriptor form;

    public TreeGroupNode(TreeGroupDescriptor group, FormDescriptor form) {
        super(group);

        this.form = form;
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new TreeGroupEditor(getTypedObject(), form);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return getSiblingNode(info) != null;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return form.moveTreeGroup(getSiblingNode(info).getTypedObject(), getTypedObject());
    }
}
