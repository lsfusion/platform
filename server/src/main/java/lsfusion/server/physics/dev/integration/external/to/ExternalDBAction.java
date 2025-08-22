package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.DefaultSQLSyntax;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ConnectionService;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ExternalDBAction extends CallDBAction {
    public ExternalDBAction(ImList<Type> params, ImList<LP> targetPropList) {
        super(2, params, targetPropList); //connection string, command + params
    }

    public void readJDBC(ExecutionContext<PropertyInterface> context, String connectionString, DBManager dbManager) throws SQLException, SQLHandledException {
        if (connectionString.equals("LOCAL"))
            throw new UnsupportedOperationException("EXTERNAL SQL 'LOCAL' is not supported, Use INTERNAL DB instead");

        SQLSyntax syntax = DefaultSQLSyntax.getSyntax(connectionString);

        Connection conn = null;
        ConnectionService connectionService = context.getConnectionService();
        if(connectionService != null)
            conn = connectionService.getSQLConnection(connectionString);
        else if (connectionString.isEmpty())
            throw new UnsupportedOperationException("Empty connection string is supported only inside of NEWCONNECTION operator");

        if (conn == null) {
            conn = DriverManager.getConnection(connectionString);
            if (connectionService != null)
                connectionService.putSQLConnection(connectionString, conn);
        }
        conn.setReadOnly(false);

        try {
            readJDBC(context, conn, syntax, OperationOwner.unknown);
        } catch (IOException | ExecutionException e) {
            throw Throwables.propagate(e);
        } finally {
            if (connectionService == null)
                conn.close();
        }
    }
}