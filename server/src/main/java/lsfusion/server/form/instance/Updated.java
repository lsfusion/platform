package lsfusion.server.form.instance;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.logics.property.CalcProperty;

public interface Updated {

    // изменилось что-то влияющее на isInInterface/getClassSet (класс верхних объектов или класс grid'а)
    boolean classUpdated(ImSet<GroupObjectInstance> gridGroups);
    // изменилось что-то использующее в getExpr конкретные value (один из верхних объектов)
    boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups);
    boolean dataUpdated(FunctionSet<CalcProperty> changedProps);
    
    void fillProperties(MSet<CalcProperty> properties);

    boolean isInInterface(GroupObjectInstance classGroup);
}
