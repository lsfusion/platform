package platform.client.descriptor.nodes;

import platform.client.descriptor.increment.IncrementDependency;
import platform.client.tree.ClientTree;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;

import javax.swing.*;
import java.util.List;

public class PropertyDrawFolder extends GroupElementFolder<PropertyDrawFolder> {
    private FormDescriptor form;

    public PropertyDrawFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, FormDescriptor form) {
        super(group, "Свойства");

        this.form = form;

        for (PropertyDrawDescriptor propertyDraw : form.propertyDraws) {
            if (group == null || group.equals(propertyDraw.getGroupObject(groupList))) {
                add(new PropertyDrawNode(group, propertyDraw, form));
            }
        }

        addCollectionReferenceActions(this, "propertyDraws", new String[] {""}, new Class[] {PropertyDrawDescriptor.class});
    }

    public boolean addToPropertyDraws(PropertyDrawDescriptor propertyDraw) {
        if (groupObject != null) {
            propertyDraw.client.groupObject = groupObject.client;
            propertyDraw.toDraw = groupObject;
        }

        form.addToPropertyDraws(propertyDraw);

        IncrementDependency.update(this, "propertyDraws");
        return true;
    }

    public boolean removeFromPropertyDraws(PropertyDrawDescriptor propertyDraw) {
        form.removeFromPropertyDraws(propertyDraw);

        IncrementDependency.update(this, "propertyDraws");
        return true;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return ClientTree.getNode(info) instanceof PropertyDrawNode;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return form.movePropertyDraw((PropertyDrawDescriptor)ClientTree.getNode(info).getTypedObject(), ClientTree.getChildIndex(info));
    }
}
