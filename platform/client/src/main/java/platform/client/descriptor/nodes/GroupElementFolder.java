package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.nodes.filters.FixedFilterFolder;
import platform.client.descriptor.nodes.filters.RegularFilterGroupFolder;

import javax.swing.tree.DefaultMutableTreeNode;

public class GroupElementFolder<C extends GroupElementFolder> extends PlainTextNode<C> {

    GroupObjectDescriptor groupObject;

    public GroupElementFolder(GroupObjectDescriptor groupObject, String caption) {
        super(caption);
        this.groupObject = groupObject;
    }

    public static void addFolders(DefaultMutableTreeNode treeNode, GroupObjectDescriptor group, FormDescriptor form) {
        treeNode.add(new PropertyDrawFolder(form.groupObjects, group, form));
        treeNode.add(new FixedFilterFolder(form.groupObjects, group, form.fixedFilters));
        treeNode.add(new RegularFilterGroupFolder(form.groupObjects, group, form.regularFilterGroups));
    }
}
