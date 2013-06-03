package lsfusion.client.descriptor.nodes.filters;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.filter.FilterDescriptor;
import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ApplicationContextProvider;
import lsfusion.client.descriptor.nodes.GroupElementFolder;

import java.util.List;
import java.util.Set;

public class FixedFilterFolder extends GroupElementFolder<FixedFilterFolder> implements ApplicationContextProvider {

    private Set<FilterDescriptor> fixedFilters;

    private FormDescriptor form;

    public ApplicationContext getContext() {
        return form.getContext();
    }

    public FixedFilterFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, final FormDescriptor form, final Set<FilterDescriptor> fixedFilters) {
        super(group, ClientResourceBundle.getString("descriptor.filter.fixed.filters"));

        this.form = form;
        this.fixedFilters = fixedFilters;

        for (FilterDescriptor filter : fixedFilters) {
            if (group == null || group.equals(filter.getGroupObject(groupList))) {
                add(filter.createNode(group));
            }
        }

        addCollectionReferenceActions(this, "fixedFilters", FilterDescriptor.derivedNames, FilterDescriptor.derivedClasses);
    }

    public Set<FilterDescriptor> getFixedFilters() {
        return fixedFilters;
    }

    public void addToFixedFilters(FilterDescriptor filter) {
        fixedFilters.add(filter);
        form.updateDependency(this, "fixedFilters");
    }

    public void removeFromFixedFilters(FilterDescriptor filter) {
        fixedFilters.remove(filter);
        form.updateDependency(this, "fixedFilters");
    }
}
