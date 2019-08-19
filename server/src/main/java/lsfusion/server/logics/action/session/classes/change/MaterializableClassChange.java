package lsfusion.server.logics.action.session.classes.change;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.table.SingleKeyPropertyUsage;
import lsfusion.server.logics.classes.user.BaseClass;

import java.sql.SQLException;
import java.util.function.Function;

public class MaterializableClassChange {
    public ClassChange change;
    public SingleKeyPropertyUsage table;

    public MaterializableClassChange(ClassChange change) {
        this.change = change;
    }

    public void materializeIfNeeded(String debugInfo, SQLSession sql, BaseClass baseClass, QueryEnvironment env, Function<ClassChange, Boolean> needMaterialize) throws SQLException, SQLHandledException {
        if(table == null && needMaterialize.apply(change)) {
            table = change.materialize(debugInfo, sql, baseClass, env); // materialize'им изменение
            change = table.getChange();
        }            
    }
    
    public void drop(SQLSession sql, OperationOwner owner) throws SQLException {
        if(table != null)
            table.drop(sql, owner);
    }
}
