package platform.client.descriptor.nodes;

import platform.client.tree.ClientTreeNode;
import platform.client.logics.ClientComponent;

public class ComponentNode<T extends ClientComponent, C extends ComponentNode> extends ClientTreeNode<T, C> {

    public ComponentNode(T component) {
        super(component);
    }
}
