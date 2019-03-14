package lsfusion.server.logics.form.interactive.change;

import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;

public interface ReallyChanged {
    
    boolean containsChange(PropertyObjectInstance instance);

    void addChange(PropertyObjectInstance instance);
}
