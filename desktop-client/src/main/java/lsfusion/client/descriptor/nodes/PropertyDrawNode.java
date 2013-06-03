package lsfusion.client.descriptor.nodes;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.PropertyDrawDescriptor;
import lsfusion.client.descriptor.editor.PropertyDrawEditor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;
import lsfusion.client.tree.ClientTree;

import javax.swing.*;

public class PropertyDrawNode extends GroupElementNode<PropertyDrawDescriptor, PropertyDrawNode> implements EditableTreeNode {

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

    public NodeEditor createEditor(FormDescriptor form) {
        return new PropertyDrawEditor(groupObject, getTypedObject(), this.form);
    }
}
