package platform.client.descriptor.nodes;

import platform.client.ClientTree;
import platform.client.ClientTreeNode;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.PropertyDrawEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public class PropertyDrawNode extends GroupElementNode<PropertyDrawDescriptor> {

    public PropertyDrawNode(GroupObjectDescriptor groupObject, PropertyDrawDescriptor userObject) {
        super(groupObject, userObject);
    }

    public JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new PropertyDrawEditor(groupObject, getDescriptor(), form, remote);
    }

    private PropertyDrawNode getPropertyNode(TransferHandler.TransferSupport info) {

        ClientTreeNode treeNode = ClientTree.getNode(info);
        if (treeNode == null) return null;

        return (PropertyDrawNode)treeNode;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {

        PropertyDrawNode propertyNode = getPropertyNode(info);
        return (propertyNode != null) && (getParent() instanceof PropertyDrawFolder) && (getParent() == propertyNode.getParent());
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return ((PropertyDrawFolder)getParent()).moveChild(getPropertyNode(info), this);
    }
}
