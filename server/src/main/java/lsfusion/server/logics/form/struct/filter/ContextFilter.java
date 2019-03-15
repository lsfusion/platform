package lsfusion.server.logics.form.struct.filter;

import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;

public interface ContextFilter {
    
    FilterInstance getFilter(InstanceFactory factory);
}
