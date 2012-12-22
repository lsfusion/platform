package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;

import java.sql.SQLException;

public abstract class FlowActionProperty extends ActionProperty<PropertyInterface> {

    public static ImOrderSet<PropertyInterface> genInterfaces(int size) {
        return SetFact.toOrderExclSet(size, genInterface);
    }

    protected <I extends PropertyInterface> FlowActionProperty(String sID, String caption, int size) {
        super(sID, caption, genInterfaces(size));
    }

    @Override
    public abstract FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException;

    public static <P extends PropertyInterface, M extends  PropertyInterface> FlowResult execute(ExecutionContext<PropertyInterface> context, ActionPropertyMapImplement<P, M> implement, ImMap<M, DataObject> keys, ImRevMap<PropertyInterface, M> mapInterfaces) throws SQLException {
        return implement.property.execute(context.override(implement.mapping.join(keys),
                BaseUtils.<ImMap<P, CalcPropertyInterfaceImplement<PropertyInterface>>>immutableCast(MapFact.innerCrossValues(implement.mapping, mapInterfaces))));
    }
}
