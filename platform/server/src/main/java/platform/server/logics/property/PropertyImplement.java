package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.logics.control.ControlImplement;

import java.util.HashMap;
import java.util.Map;

public class PropertyImplement<T,P extends PropertyInterface> extends ControlImplement<T, P, Property<P>> {

    public PropertyImplement(Property<P> property,Map<P,T> mapping) {
        super(property, mapping);
    }

    public PropertyImplement(Property<P> property) {
        super(property);
    }

    public <L> PropertyImplement<L, P> mapImplement(Map<T,L> mapImplement) {
        return new PropertyImplement<L,P>(property,BaseUtils.join(mapping,mapImplement));
    }
}
