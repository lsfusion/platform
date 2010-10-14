package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;

import java.util.List;

public class PropertyDrawFolder extends GroupElementFolder<PropertyDrawFolder> {

    public PropertyDrawFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, FormDescriptor form) {
        super(group, "Свойства");

        for (PropertyDrawDescriptor propertyDraw : form.propertyDraws)
            if (group==null || group.equals(propertyDraw.getGroupObject(groupList)))
                add(new PropertyDrawNode(group, propertyDraw, form));
    }
}
