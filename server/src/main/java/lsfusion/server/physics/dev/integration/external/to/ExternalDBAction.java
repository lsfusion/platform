package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.mutability.MutableObject;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.connection.ExConnection;
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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ExternalDBAction extends CallDBAction {
    public ExternalDBAction(ImList<Type> params, ImList<LP> targetPropList) {
        super(2, params, targetPropList); //connection string, command + params
    }

    public List<Object> readJDBC(ExecutionContext<PropertyInterface> context, String connectionString, DBManager dbManager) throws SQLException, SQLHandledException {
        SQLSyntax syntax;
        OperationOwner owner = OperationOwner.unknown;

        boolean isLocalDB = connectionString.equals("LOCAL");
        MutableObject connOwner = null;
        ExConnection exConn = null;
        boolean prevReadOnly = false;
        Connection conn;
        if(isLocalDB) { //deprecated, use INTERNAL DB
            DataAdapter adapter = dbManager.getAdapter();
            syntax = adapter.syntax;
            connOwner = new MutableObject();
            exConn = adapter.getPrivate(connOwner, dbManager.contextProvider);
            conn = exConn.sql;
            prevReadOnly = conn.isReadOnly();
        } else {
            syntax = DefaultSQLSyntax.getSyntax(connectionString);
            conn = DriverManager.getConnection(connectionString);
        }

        try {
            return readJDBC(context, conn, syntax, owner);
        } catch (IOException | ExecutionException e) {
            throw Throwables.propagate(e);
        } finally {
            if (conn != null) {
                if(isLocalDB) {
                    conn.setReadOnly(prevReadOnly);
                    dbManager.getAdapter().returnPrivate(connOwner, exConn);
                } else
                    conn.close();
            }
        }
    }
}