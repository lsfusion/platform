package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.file.FileData;
import lsfusion.base.mutability.MutableObject;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLCommand;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.connection.ExConnection;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.statement.ParsedStatement;
import lsfusion.server.data.sql.syntax.DefaultSQLSyntax;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.parse.AbstractParseInterface;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.file.DynamicFormatFileClass;
import lsfusion.server.logics.form.stat.struct.plain.JDBCTable;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ExternalDBAction extends ExternalAction {

    private PropertyInterface connectionString;
    private PropertyInterface exec;

    public ExternalDBAction(ImList<Type> params, ImList<LP> targetPropList) {
        super(2, params, targetPropList); // строка подключения, команда + параметры

        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        connectionString = orderInterfaces.get(0);
        exec = orderInterfaces.get(1);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        String replacedParams = replaceParams(context, getTransformedText(context, connectionString));
        List<Object> results = readJDBC(context.getKeys(), replacedParams, getTransformedText(context, exec), context.getDbManager());

        for (int i = 0; i < targetPropList.size(); i++)
            targetPropList.get(i).change(results.get(i), context);

        return FlowResult.FINISH;
    }

    private List<Object> readJDBC(ImMap<PropertyInterface, ? extends ObjectValue> params, String connectionString, String exec, DBManager dbManager) throws SQLException, SQLHandledException {
        SQLSyntax syntax;
        OperationOwner owner = OperationOwner.unknown;

        boolean isLocalDB = connectionString.equals("LOCAL");
        MutableObject connOwner = null;
        ExConnection exConn = null;
        boolean prevReadOnly = false;
        Connection conn;
        if(isLocalDB) {
            DataAdapter adapter = dbManager.getAdapter();
            syntax = adapter.syntax;
            connOwner = new MutableObject();
            exConn = adapter.getPrivate(connOwner);
            conn = exConn.sql;
            prevReadOnly = conn.isReadOnly();
        } else {
            syntax = DefaultSQLSyntax.getSyntax(connectionString);
            conn = DriverManager.getConnection(connectionString);
        }
        List<String> tempTables = new ArrayList<>();

        try {
            conn.setReadOnly(false);

            int tableParamNum = 0;
            ImOrderSet<PropertyInterface> orderInterfaces = paramInterfaces;
            MExclMap<String, ParseInterface> mParamObjects = MapFact.mExclMap(orderInterfaces.size());
            for(int i=0,size=orderInterfaces.size();i<size;i++) {
                PropertyInterface param = orderInterfaces.get(i);
                ObjectValue paramValue = params.get(param);

                ParseInterface parse = null;

                if(paramValue instanceof DataObject) {
                    DataClass paramClass = (DataClass) ((DataObject) paramValue).objectClass;
                    if (paramClass instanceof DynamicFormatFileClass) {
                        DynamicFormatFileClass fileParamClass = (DynamicFormatFileClass) paramClass;
                        FileData fileData = fileParamClass.read(paramValue.getValue());
                        String extension = fileData.getExtension();
                        if (extension.equals("jdbc")) { // значит таблица
                            JDBCTable jdbcTable = JDBCTable.deserializeJDBC(fileData.getRawFile());

                            String table = "ti_" + tableParamNum; // создаем временную таблицу с сгенерированным именем
                            SQLSession.uploadTableToConnection(table, syntax, jdbcTable, conn, owner);
                            tempTables.add(table);
                            parse = SessionTable.getParseInterface(table);
                        }
                    }
                    if(parse == null)
                        parse = paramValue.getParse(paramClass.getType(), syntax);
                } else
                    parse = AbstractParseInterface.SAFENULL; // тут получается, что типа нет, но он и не нужен (STRUCT'ы не имеют смысла, за cast типов отвечает сама внешняя SQL команда, все safe соответственно ни writeParam, ни getType вызваться не могут / не должны)

                mParamObjects.exclAdd(getParamName(String.valueOf(i+1)), parse);
            }
            ImMap<String, ParseInterface> paramObjects = mParamObjects.immutable();

            ParsedStatement parsed = SQLCommand.preparseStatement(exec, paramObjects, syntax).parseStatement(conn, syntax);

            try {
                SQLSession.ParamNum paramNum = new SQLSession.ParamNum();
                for (String param : parsed.preparedParams)
                    paramObjects.get(param).writeParam(parsed.statement, paramNum, syntax);

                boolean isResultSet = (boolean) Executors.newSingleThreadExecutor().submit((Callable) parsed.statement::execute).get();

                List<Object> results = new ArrayList<>();
                while(true) {
                    if(isResultSet) results.add(new FileData(JDBCTable.serialize(parsed.statement.getResultSet()), "jdbc"));
                    else {
                        int updateCount = parsed.statement.getUpdateCount();
                        if(updateCount == -1)
                            break;
                        else
                            results.add(updateCount);
                    }
                    isResultSet = parsed.statement.getMoreResults();
                }
                return results;
            }  catch (InterruptedException e) {
                parsed.statement.cancel();
                throw Throwables.propagate(e);
            }finally {
                parsed.statement.close();
            }
        } catch (IOException | ExecutionException e) {
            throw Throwables.propagate(e);
        } finally {
            for(String table : tempTables)
                SQLSession.dropTemporaryTableFromDB(conn, syntax, table, owner);
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