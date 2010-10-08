package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;

import javax.swing.tree.DefaultMutableTreeNode;

public class GroupElementFolder extends DefaultMutableTreeNode {

    GroupObjectDescriptor groupObject;

    public GroupElementFolder(GroupObjectDescriptor groupObject, String caption) {
        super(caption);
        this.groupObject = groupObject;
    }

    public static void addFolders(DefaultMutableTreeNode treeNode, GroupObjectDescriptor group, FormDescriptor form) {
        treeNode.add(new PropertyDrawFolder(form.groups, group, form.propertyDraws));
        treeNode.add(new FixedFilterFolder(form.groups, group, form.fixedFilters));
        treeNode.add(new RegularFilterGroupFolder(form.groups, group, form.regularFilterGroups));
    }
}
