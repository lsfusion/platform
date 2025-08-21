package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.mutability.MutableObject;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.connection.ExConnection;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class InternalDBAction extends CallDBAction {

    public InternalDBAction(ImList<Type> params, ImList<LP> targetPropList) {
        super(1, params, targetPropList); //command + params
    }

    public void readJDBC(ExecutionContext<PropertyInterface> context, String connectionString, DBManager dbManager) throws SQLException, SQLHandledException {
        SQLSession sql = context.getSession().sql;
        sql.pushNoReadOnly();
        try {
            ExConnection exConnection = sql.getConnection();
            try {
                readJDBC(context, exConnection.sql, sql.syntax, OperationOwner.unknown);
            } catch (IOException | ExecutionException e) {
                throw Throwables.propagate(e);
            } finally {
                sql.returnConnection(exConnection, OperationOwner.unknown);
            }
        } finally {
            sql.popNoReadOnly();
        }
    }
}