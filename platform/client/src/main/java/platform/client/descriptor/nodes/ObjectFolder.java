package platform.client.descriptor.nodes;

import platform.client.descriptor.ObjectDescriptor;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class ObjectFolder extends DefaultMutableTreeNode {

    public ObjectFolder(List<ObjectDescriptor> objects) {
        super("Объекты");

        for (ObjectDescriptor object : objects)
            add(new ObjectNode(object));
    }
}
