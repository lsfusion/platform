package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;

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

    public static <P extends PropertyInterface, M extends  PropertyInterface> FlowResult execute(ExecutionContext<PropertyInterface> context, ActionPropertyMapImplement<P, M> implement, ImMap<M, ? extends ObjectValue> keys, ImRevMap<PropertyInterface, M> mapInterfaces) throws SQLException {
        return implement.property.execute(context.override(implement.mapping.join(keys),
                BaseUtils.<ImMap<P, CalcPropertyInterfaceImplement<PropertyInterface>>>immutableCast(MapFact.innerCrossValues(implement.mapping, mapInterfaces))));
    }
}
