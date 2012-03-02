package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyInterfaceImplement;

import java.util.Collection;
import java.util.List;

public abstract class KeepContextActionProperty extends FlowActionProperty {

    protected <I extends PropertyInterface> KeepContextActionProperty(String sID, String caption, List<I> listInterfaces, Collection<PropertyInterfaceImplement<I>> used) {
        super(sID, caption, listInterfaces, used);
    }

    protected KeepContextActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }
}
