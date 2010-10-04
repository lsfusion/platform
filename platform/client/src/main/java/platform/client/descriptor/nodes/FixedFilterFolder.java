package platform.client.descriptor.nodes;

import platform.client.descriptor.filter.FilterDescriptor;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Set;

public class FixedFilterFolder extends DefaultMutableTreeNode {

    Set<FilterDescriptor> fixedFilters;
    public FixedFilterFolder(Set<FilterDescriptor> fixedFilters) {
        super("Постоянные фильтры");
        
        this.fixedFilters = fixedFilters;
        for (FilterDescriptor filter : fixedFilters)
            add(new FilterNode(filter));
    }
}
