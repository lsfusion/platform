package lsfusion.server.session;

import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;

import java.sql.SQLException;

class MaterializableClassChange {
    public ClassChange change;
    public SingleKeyPropertyUsage table;

    public MaterializableClassChange(ClassChange change) {
        this.change = change;
    }

    public void materializeIfNeeded(String debugInfo, SQLSession sql, BaseClass baseClass, QueryEnvironment env, GetValue<Boolean, ClassChange> needMaterialize) throws SQLException, SQLHandledException {
        if(table == null && needMaterialize.getMapValue(change)) {
            table = change.materialize(debugInfo, sql, baseClass, env); // materialize'им изменение
            change = table.getChange();
        }            
    }
    
    public void drop(SQLSession sql, OperationOwner owner) throws SQLException {
        if(table != null)
            table.drop(sql, owner);
    }
}
