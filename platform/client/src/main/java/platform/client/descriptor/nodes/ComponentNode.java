package platform.client.descriptor.nodes;

import platform.client.logics.ClientComponent;

import javax.swing.tree.DefaultMutableTreeNode;

public class ComponentNode extends DefaultMutableTreeNode {
    
    public ComponentNode(ClientComponent component) {
        super(component);
    }
}
