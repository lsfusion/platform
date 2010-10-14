package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;

public class GroupObjectFolder extends PlainTextNode<GroupObjectFolder> {

    public GroupObjectFolder(FormDescriptor form) {
        super("Группы объектов");

        for (GroupObjectDescriptor descriptor : form.groups) {
            add(new GroupObjectNode(descriptor, form));
        }
    }
}
