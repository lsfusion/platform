package platform.client.descriptor.nodes;

import platform.client.ClientTree;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.PropertyDrawEditor;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;

public class PropertyDrawNode extends GroupElementNode<PropertyDrawDescriptor, PropertyDrawNode> {

    public PropertyDrawNode(GroupObjectDescriptor groupObject, PropertyDrawDescriptor userObject) {
        super(groupObject, userObject);
    }

    public JComponent createEditor(FormDescriptor form, RemoteDescriptorInterface remote) {
        return new PropertyDrawEditor(groupObject, getTypedObject(), form, remote);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return (getSiblingNode(info) != null) && (getParent() instanceof PropertyDrawFolder);
    }

    @Override
    public void importData(ClientTree tree, TransferHandler.TransferSupport info) {
        ((PropertyDrawFolder)getParent()).moveChild(getSiblingNode(info), this);
    }
}
