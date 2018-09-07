package lsfusion.server.form.entity.filter;

import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.filter.FilterInstance;

public interface ContextFilter {
    
    FilterInstance getFilter(InstanceFactory factory);
}
