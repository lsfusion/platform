package lsfusion.client.descriptor.nodes;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.nodes.filters.FixedFilterFolder;
import lsfusion.client.descriptor.nodes.filters.RegularFilterGroupFolder;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class GroupElementFolder<C extends GroupElementFolder> extends PlainTextNode<C> {

    GroupObjectDescriptor groupObject;

    public GroupElementFolder(GroupObjectDescriptor groupObject, String caption) {
        super(caption);
        this.groupObject = groupObject;
    }

    public static void addFolders(DefaultMutableTreeNode treeNode, GroupObjectDescriptor group, FormDescriptor form) {
        treeNode.add(new PropertyDrawFolder(group, form));
        treeNode.add(new FixedFilterFolder(form.groupObjects, group, form, form.fixedFilters));
        treeNode.add(new RegularFilterGroupFolder(form.groupObjects, group, form));
    }
}
