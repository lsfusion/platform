package platform.server.logics.property.actions.flow;

import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyInterfaceImplement;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class ExtendContextActionProperty<I extends PropertyInterface> extends FlowActionProperty {

    protected final Collection<I> innerInterfaces;
    protected final Map<ClassPropertyInterface, I> mapInterfaces;

    public ExtendContextActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, Collection<? extends PropertyInterfaceImplement<I>> used) {
        super(sID, caption, mapInterfaces, used);

        this.innerInterfaces = innerInterfaces;
        this.mapInterfaces = getMapInterfaces(mapInterfaces);
    }
}
