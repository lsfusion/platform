package platform.client.descriptor.nodes.filters;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.filter.FilterDescriptor;
import platform.interop.context.ApplicationContext;
import platform.interop.context.ApplicationContextProvider;
import platform.interop.context.IncrementDependency;
import platform.client.descriptor.nodes.GroupElementFolder;

import java.util.List;
import java.util.Set;

public class FixedFilterFolder extends GroupElementFolder<FixedFilterFolder> implements ApplicationContextProvider {

    private Set<FilterDescriptor> fixedFilters;

    private FormDescriptor form;

    public ApplicationContext getContext() {
        return form.getContext();
    }

    public FixedFilterFolder(List<GroupObjectDescriptor> groupList, GroupObjectDescriptor group, final FormDescriptor form, final Set<FilterDescriptor> fixedFilters) {
        super(group, "Постоянные фильтры");

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
