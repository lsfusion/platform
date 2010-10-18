package platform.client.descriptor.nodes;

import platform.base.BaseUtils;
import platform.client.ClientTree;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.nodes.actions.AddableTreeNode;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class PropertyDrawFolder extends GroupElementFolder<PropertyDrawFolder> implements AddableTreeNode {
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

    public Object[] addNewElement(TreePath selectionPath) {
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

        return BaseUtils.add( ClientTree.convertTreePathToUserObjects(selectionPath), propertyDraw );
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
