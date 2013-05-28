package platform.server.form.instance;

import platform.base.FunctionSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.logics.property.CalcProperty;

public interface Updated {

    // изменилось что-то влияющее на isInInterface/getClassSet (класс верхних объектов или класс grid'а)
    boolean classUpdated(ImSet<GroupObjectInstance> gridGroups);
    // изменилось что-то использующее в getExpr конкретные value (один из верхних объектов)
    boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups);
    boolean dataUpdated(FunctionSet<CalcProperty> changedProps);
    
    void fillProperties(MSet<CalcProperty> properties);

    boolean isInInterface(GroupObjectInstance classGroup);
}
