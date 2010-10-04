package platform.client.descriptor.nodes;

import javax.swing.tree.DefaultMutableTreeNode;

public class ObjectNode extends DefaultMutableTreeNode {

    public ObjectNode(Object userObject) {
        super(userObject, false);
    }
}
