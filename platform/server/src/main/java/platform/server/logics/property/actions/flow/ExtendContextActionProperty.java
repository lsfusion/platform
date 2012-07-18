package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.logics.property.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class ExtendContextActionProperty<I extends PropertyInterface> extends FlowActionProperty {

    protected final Collection<I> innerInterfaces;
    protected final Map<PropertyInterface, I> mapInterfaces;

    public ExtendContextActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces) {
        super(sID, caption, mapInterfaces.size());

        this.innerInterfaces = innerInterfaces;
        this.mapInterfaces = getMapInterfaces(mapInterfaces);
    }

    @IdentityLazy
    public CalcPropertyMapImplement<?, PropertyInterface> getWhereProperty() {
        return IsClassProperty.getMapProperty(BaseUtils.join(mapInterfaces, // по аналогии с группировкой (а точнее вместо) такая "эвристика"
                getGroupWhereProperty().mapInterfaceCommonClasses(null)));
    }
    protected abstract CalcPropertyMapImplement<?, I> getGroupWhereProperty();

    public ActionPropertyMapImplement<PropertyInterface, I> getMapImplement() {
        return new ActionPropertyMapImplement<PropertyInterface, I>(this, mapInterfaces);
    }

}
