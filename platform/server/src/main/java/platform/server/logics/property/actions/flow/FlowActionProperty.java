package platform.server.logics.property.actions.flow;

import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyInterfaceImplement;

import java.util.Collection;
import java.util.List;

public abstract class FlowActionProperty extends ActionProperty {

    protected <I extends PropertyInterface> FlowActionProperty(String sID, String caption, List<I> listInterfaces, Collection<PropertyInterfaceImplement<I>> used) {
        super(sID, caption, getClasses(listInterfaces, used));
    }
}
