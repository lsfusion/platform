package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.ContainerEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeNode;

import javax.swing.*;

public class ContainerNode extends ComponentNode<ClientContainer, ContainerNode> implements EditableTreeNode {

    public ContainerNode(ClientContainer container) {
        super(container, true);

        for (ClientComponent child : container.children) {
            add(child.getNode());
        }

        addCollectionReferenceActions(container, "children", new String[]{""}, new Class[]{ClientContainer.class});
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        ClientTreeNode node = ClientTree.getNode(info);
        return node instanceof ComponentNode && !node.isNodeDescendant(this);
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        ClientTreeNode node = ClientTree.getNode(info);
        if (node instanceof ComponentNode && !node.isNodeDescendant(this)) {
            ClientComponent component = (ClientComponent) node.getUserObject();

            ContainerNode parent = (ContainerNode) node.getParent();

            int index = ClientTree.getChildIndex(info);

            if (index != -1 && getTypedObject().equals(parent.getTypedObject()) && getTypedObject().children.indexOf(component) < index) {
                index--;
            }

            parent.getTypedObject().removeFromChildren(component);

            if (index == -1) {
                getTypedObject().addToChildren(component);
            } else {
                getTypedObject().addToChildren(index, component);
            }

            return true;
        } else {
            return false;
        }
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new ContainerEditor(getTypedObject());
    }
}
