package platform.server.form.instance;

import platform.server.form.instance.GroupObjectInstance;
import platform.server.logics.property.Property;

import java.util.Collection;
import java.util.Set;

public interface Updated {

    // изменилось что-то влияющее на isInInterface/getClassSet (класс верхних объектов или класс grid'а)
    boolean classUpdated(GroupObjectInstance classGroup);
    // изменилось что-то использующее в getExpr конкретные value (один из верхних объектов)
    boolean objectUpdated(Set<GroupObjectInstance> skipGroups);
    boolean dataUpdated(Collection<Property> changedProps);
    
    void fillProperties(Set<Property> properties);

    boolean isInInterface(GroupObjectInstance classGroup);
}
