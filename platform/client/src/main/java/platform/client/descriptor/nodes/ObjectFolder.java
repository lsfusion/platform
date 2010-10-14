package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;

public class ObjectFolder extends PlainTextNode<ObjectFolder> {

    private GroupObjectDescriptor group;

    public ObjectFolder(GroupObjectDescriptor group) {
        super("Oбъекты");

        this.group = group;

        for (ObjectDescriptor object : group)
            add(new ObjectNode(object));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectFolder that = (ObjectFolder) o;

        if (group != null ? !group.equals(that.group) : that.group != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return group != null ? group.hashCode() : 0;
    }
}
