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

    public ClientContainer getClientContainer() {
        return (ClientContainer) getUserObject();
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

            parent.getClientContainer().removeChild(component);

            int index = ClientTree.getChildIndex(info);
            if (index == -1) {
                this.getClientContainer().addChild(component);
            } else {
                this.getClientContainer().addChild(index, component);
            }

            return true;
        } else
            return false;
    }
}
