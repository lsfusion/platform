package platform.client.descriptor.nodes.filters;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.RegularFilterGroupDescriptor;
import platform.client.descriptor.nodes.GroupElementFolder;
import platform.base.context.ApplicationContext;
import platform.base.context.ApplicationContextProvider;

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
