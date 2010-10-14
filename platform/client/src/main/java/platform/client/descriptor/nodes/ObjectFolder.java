package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;

public class ObjectFolder extends PlainTextNode<ObjectFolder> {

    public ObjectFolder(GroupObjectDescriptor group) {
        super("Oбъекты");

        for (ObjectDescriptor object : group)
            add(new ObjectNode(object, group));
    }
}
