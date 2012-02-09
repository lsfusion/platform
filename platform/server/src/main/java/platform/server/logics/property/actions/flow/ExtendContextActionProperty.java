package platform.server.logics.property.actions.flow;

import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.join;

public abstract class ExtendContextActionProperty<I extends PropertyInterface> extends FlowActionProperty {

    protected final Collection<I> innerInterfaces;
    protected final Map<ClassPropertyInterface, I> mapInterfaces;

    public ExtendContextActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, Collection<PropertyInterfaceImplement<I>> used) {
        super(sID, caption, mapInterfaces, used);

        this.innerInterfaces = innerInterfaces;
        this.mapInterfaces = getMapInterfaces(mapInterfaces);
    }

    protected static <M> void execute(PropertyImplement<ClassPropertyInterface, M> implement, Map<M, DataObject> values,  ExecutionContext context) throws SQLException {
        ((ActionProperty)implement.property).execute(context.override(join(implement.mapping, values)));
    }
}
