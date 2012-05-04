package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.TreeGroupDescriptor;
import platform.client.descriptor.editor.TreeGroupEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.tree.ClientTree;

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
