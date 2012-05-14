package platform.server.form.instance;

import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.Property;

import java.util.Collection;
import java.util.Set;

public interface Updated {

    // изменилось что-то влияющее на isInInterface/getClassSet (класс верхних объектов или класс grid'а)
    boolean classUpdated(Set<GroupObjectInstance> gridGroups);
    // изменилось что-то использующее в getExpr конкретные value (один из верхних объектов)
    boolean objectUpdated(Set<GroupObjectInstance> gridGroups);
    boolean dataUpdated(Collection<CalcProperty> changedProps);
    
    void fillProperties(Set<CalcProperty> properties);

    boolean isInInterface(GroupObjectInstance classGroup);
}
