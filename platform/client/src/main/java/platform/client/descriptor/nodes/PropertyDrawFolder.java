package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.nodes.actions.AddingTreeNode;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class PropertyDrawFolder extends GroupElementFolder<PropertyDrawFolder> implements AddingTreeNode {
    private FormDescriptor form;

    public PropertyDrawFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, FormDescriptor form) {
        super(group, "Свойства");

        this.form = form;

        for (PropertyDrawDescriptor propertyDraw : form.propertyDraws) {
            if (group == null || group.equals(propertyDraw.getGroupObject(groupList))) {
                add(new PropertyDrawNode(group, propertyDraw, form));
            }
        }
    }

    public void addNewElement(TreePath selectionPath) {
        ClientPropertyDraw clientPropertyDraw = new ClientPropertyDraw();
        clientPropertyDraw.caption = "Новое свойство";
        clientPropertyDraw.columnGroupObjects = new ArrayList<ClientGroupObject>();

        PropertyDrawDescriptor propertyDraw = new PropertyDrawDescriptor();
        propertyDraw.setColumnGroupObjects(new ArrayList<GroupObjectDescriptor>());
        propertyDraw.client = clientPropertyDraw;

        if (groupObject != null) {
            clientPropertyDraw.groupObject = groupObject.client;
            propertyDraw.toDraw = groupObject;
        }

        form.addPropertyDraw(propertyDraw);
    }
}
