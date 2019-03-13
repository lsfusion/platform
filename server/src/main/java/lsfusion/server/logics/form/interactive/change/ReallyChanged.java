package lsfusion.server.logics.form.interactive.change;

import lsfusion.server.logics.form.interactive.instance.property.CalcPropertyObjectInstance;

public interface ReallyChanged {
    
    boolean containsChange(CalcPropertyObjectInstance instance);

    void addChange(CalcPropertyObjectInstance instance);
}
