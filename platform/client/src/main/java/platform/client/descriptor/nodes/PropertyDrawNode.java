package platform.client.descriptor.nodes;

import platform.client.tree.ClientTree;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.PropertyDrawEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.nodes.actions.DeletableTreeNode;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import javax.swing.tree.TreePath;

public class PropertyDrawNode extends GroupElementNode<PropertyDrawDescriptor, PropertyDrawNode> implements EditableTreeNode, DeletableTreeNode {

    private FormDescriptor form;

    public PropertyDrawNode(GroupObjectDescriptor groupObject, PropertyDrawDescriptor userObject, FormDescriptor form) {
        super(groupObject, userObject);

        this.form = form;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return getSiblingNode(info) != null;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return form.movePropertyDraw(getSiblingNode(info).getTypedObject(), getTypedObject());
    }

    public NodeEditor createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new PropertyDrawEditor(groupObject, getTypedObject(), this.form, remote);
    }

    public boolean deleteNode(TreePath selectionPath) {
        return form.removePropertyDraw(getTypedObject());
    }
}
