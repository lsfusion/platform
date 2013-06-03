package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.filter.RegularFilterGroupDescriptor;
import lsfusion.client.descriptor.nodes.GroupElementFolder;
import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ApplicationContextProvider;

import java.util.List;

public class RegularFilterGroupFolder extends GroupElementFolder<RegularFilterGroupFolder> implements ApplicationContextProvider {

    private FormDescriptor form;

    public ApplicationContext getContext() {
        return form.getContext();
    }

    public RegularFilterGroupFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, final FormDescriptor form) {
        super(group, ClientResourceBundle.getString("descriptor.filter.regular.filters"));

        this.form = form;

        for (RegularFilterGroupDescriptor filter : form.regularFilterGroups)
            if(group==null || group.equals(filter.getGroupObject(groupList)))
                add(new RegularFilterGroupNode(group, filter));

        addCollectionReferenceActions(form, "regularFilterGroups", new String[] {""}, new Class[] {RegularFilterGroupDescriptor.class});
    }
}
