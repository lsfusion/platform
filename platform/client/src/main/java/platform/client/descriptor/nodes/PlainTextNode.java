package platform.client.descriptor.nodes;

import platform.client.ClientTreeNode;

import javax.swing.tree.DefaultMutableTreeNode;

public class PlainTextNode<C extends PlainTextNode> extends ClientTreeNode<String, C> {
    public PlainTextNode(String caption) {
        super(caption);
    }

    @Override
    public String toString() {
        return getTypedObject();
    }
}
