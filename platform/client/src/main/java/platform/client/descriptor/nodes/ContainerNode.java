package platform.client.descriptor.nodes;

import platform.client.logics.ClientContainer;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class ContainerNode extends DefaultMutableTreeNode {

    public ContainerNode(ClientContainer container, List<ClientContainer> containers) {
        super(container);

        for (ClientContainer child : containers)
            if (container.equals(child.container))
                add(new ContainerNode(child, containers));
    }
}
