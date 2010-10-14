package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;

import java.util.List;

public class PropertyDrawFolder extends GroupElementFolder {

    private GroupObjectDescriptor group;
    private FormDescriptor form;

    public PropertyDrawFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, FormDescriptor form) {
        super(group, null);

        this.group = group;
        this.form = form;

        this.setUserObject(this);

        for (PropertyDrawDescriptor propertyDraw : form.propertyDraws)
            if (group==null || group.equals(propertyDraw.getGroupObject(groupList)))
                add(new PropertyDrawNode(group, propertyDraw));
    }

    public void moveChild(PropertyDrawNode nodeFrom, PropertyDrawNode nodeTo) {
        form.movePropertyDraw(nodeFrom.getTypedObject(), nodeTo.getTypedObject());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyDrawFolder that = (PropertyDrawFolder) o;

        if (group != null ? !group.equals(that.group) : that.group != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return group != null ? group.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Свойства";
    }
}
