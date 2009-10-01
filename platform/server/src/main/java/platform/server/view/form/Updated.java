package platform.server.view.form;

import platform.server.logics.properties.Property;

import java.util.Collection;
import java.util.Set;

public interface Updated {

    // изменилось что-то влияющее на isInInterface/getClassSet (класс верхних объектов или класс grid'а)
    boolean classUpdated(GroupObjectImplement classGroup);
    // изменилось что-то использующее в getSourceExpr конкретные value (один из верхних объектов)
    boolean objectUpdated(GroupObjectImplement classGroup);
    boolean dataUpdated(Collection<Property> changedProps);
    void fillProperties(Set<Property> properties);

    boolean isInInterface(GroupObjectImplement classGroup);
}
