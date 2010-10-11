package platform.client.descriptor.nodes;

import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class ContainerNode extends ComponentNode {

    public ContainerNode(ClientContainer container) {
        super(container);

        for (ClientComponent child : container.children)
            add(child.getNode());
    }
}
