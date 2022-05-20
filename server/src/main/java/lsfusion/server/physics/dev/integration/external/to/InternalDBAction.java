package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.mutability.MutableObject;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.connection.ExConnection;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
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

    public List<Object> readJDBC(ImMap<PropertyInterface, ? extends ObjectValue> params, String connectionString, String exec, DBManager dbManager) throws SQLException, SQLHandledException {
        OperationOwner owner = OperationOwner.unknown;

        DataAdapter adapter = dbManager.getAdapter();
        SQLSyntax syntax = adapter.syntax;
        MutableObject connOwner = new MutableObject();
        ExConnection exConn = adapter.getPrivate(connOwner);
        Connection conn = exConn.sql;
        boolean prevReadOnly = conn.isReadOnly();
        List<String> tempTables = new ArrayList<>();

        try {
            return readJDBC(params, exec, conn, syntax, owner, tempTables);
        } catch (IOException | ExecutionException e) {
            throw Throwables.propagate(e);
        } finally {
            for(String table : tempTables)
                SQLSession.dropTemporaryTableFromDB(conn, syntax, table, owner);
            conn.setReadOnly(prevReadOnly);
            dbManager.getAdapter().returnPrivate(connOwner, exConn);
        }
    }
}