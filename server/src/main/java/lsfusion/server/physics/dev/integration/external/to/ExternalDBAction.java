package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.DefaultSQLSyntax;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class ExternalDBAction extends CallDBAction {
    public ExternalDBAction(ImList<Type> params, ImList<LP> targetPropList) {
        super(2, params, targetPropList); //connection string, command + params
    }

    public void readJDBC(ExecutionContext<PropertyInterface> context, String connectionString, DBManager dbManager) throws SQLException, SQLHandledException {
        if (connectionString.equals("LOCAL"))
            throw new UnsupportedOperationException("EXTERNAL SQL 'LOCAL' is not supported, Use INTERNAL DB instead");

        boolean shouldClose = context.getConnectionService() == null;
        try (ManagedConnection managedConn = new ManagedConnection(context.getSQLConnection(connectionString), shouldClose)) {
            Connection conn = managedConn.getConnection();
            conn.setReadOnly(false);
            readJDBC(context, conn, DefaultSQLSyntax.getSyntax(connectionString), OperationOwner.unknown);
        } catch (IOException | ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }
}