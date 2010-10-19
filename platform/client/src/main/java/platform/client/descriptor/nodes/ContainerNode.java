package platform.client.descriptor.nodes;

import platform.client.ClientTree;
import platform.client.ClientTreeNode;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;

import javax.swing.*;

public class ContainerNode extends ComponentNode<ClientContainer, ContainerNode> {

    public ContainerNode(ClientContainer container) {
        super(container);

        for (ClientComponent child : container.children) {
            add(child.getNode());
        }
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

            parent.getTypedObject().removeChild(component);

            if (index == -1) {
                getTypedObject().addChild(component);
            } else {
                getTypedObject().addChild(index, component);
            }

            return true;
        } else
            return false;
    }
}
