package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;

import java.sql.SQLException;

public abstract class FlowActionProperty extends ActionProperty<PropertyInterface> {

    public static ImOrderSet<PropertyInterface> genInterfaces(int size) {
        return SetFact.toOrderExclSet(size, genInterface);
    }

    protected <I extends PropertyInterface> FlowActionProperty(String caption, int size) {
        super(caption, genInterfaces(size));
    }

    @Override
    public abstract FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException;

}
