package lsfusion.server.logics.debug;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.SystemActionProperty;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;
import java.util.Map;

public class WatchActionProperty extends SystemExplicitActionProperty {
    
    public WatchActionProperty() {
        super();
    }

    public static final WatchActionProperty instance = new WatchActionProperty();
    
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        context.notifyParamValues(MapFact.<String, ObjectValue>EMPTY());
    }
}
