package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.base.ResourceUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.file.FileData;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLCommand;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.statement.ParsedStatement;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.parse.AbstractParseInterface;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.TableClass;
import lsfusion.server.logics.form.stat.struct.plain.JDBCTable;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class CallDBAction extends CallAction {

    private PropertyInterface connectionString;
    private PropertyInterface exec;

    public CallDBAction(int exParams, ImList<Type> params, ImList<LP> targetPropList) {
        super(exParams, params, targetPropList);

        boolean hasConnectionString = exParams == 2;
        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        connectionString = hasConnectionString ? orderInterfaces.get(0) : null;
        exec = orderInterfaces.get(hasConnectionString ? 1 : 0);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        readJDBC(context, connectionString != null ? replaceParams(context, getTransformedText(context, connectionString)) : null, context.getDbManager());

        return FlowResult.FINISH;
    }

    protected abstract void readJDBC(ExecutionContext<PropertyInterface> context, String connectionString, DBManager dbManager) throws SQLException, SQLHandledException;

    private ImMap<String, ParseInterface> replaceParams(ExecutionContext<PropertyInterface> context, Connection conn, SQLSyntax syntax, OperationOwner owner, List<String> tempTables) throws IOException, SQLException {
        int tableParamNum = 0;
        ImOrderSet<PropertyInterface> orderInterfaces = paramInterfaces;
        MExclMap<String, ParseInterface> mParamObjects = MapFact.mExclMap(orderInterfaces.size());
        for (int i = 0, size = orderInterfaces.size(); i < size; i++) {
            PropertyInterface param = orderInterfaces.get(i);
            ObjectValue paramValue = context.getKeyValue(param);

            ParseInterface parse = null;

            if (paramValue instanceof DataObject) {
                DataObject paramObject = (DataObject) paramValue;
                DataClass paramClass = (DataClass) getFileClass(paramObject, paramTypes.get(param));
                if (paramClass instanceof FileClass) {
                    JDBCTable jdbcTable = readTableFile(paramObject, paramClass);
                    if (jdbcTable != null) {
                        String table = "ti_" + tableParamNum; // создаем временную таблицу с сгенерированным именем
                        SQLSession.uploadTableToConnection(table, syntax, jdbcTable, conn, owner);
                        tempTables.add(table);
                        parse = SessionTable.getParseInterface(table);
                    }
                }
                if (parse == null)
                    parse = paramValue.getParse(paramClass.getType(), syntax);
            } else
                parse = AbstractParseInterface.SAFENULL; // // here it turns out that there is no type, but it is not needed (STRUCT's have no sense, the external SQL command itself is responsible for casting types, all safe respectively neither writeParam nor getType can / should not be called)

            mParamObjects.exclAdd(SQLSession.getParamName(String.valueOf(i + 1)), parse); // should match the string SQLSession.getParamName("$1") in the readJDBC (one-level up in the stack)
        }
        return mParamObjects.immutable();
    }

    protected void readJDBC(ExecutionContext<PropertyInterface> context, Connection conn, SQLSyntax syntax, OperationOwner owner) throws SQLException, SQLHandledException, IOException, ExecutionException {
        String exec = (String) context.getKeyObject(this.exec);
        boolean isFile = exec.endsWith(".sql");
        if(isFile)
            exec = ResourceUtils.findResourceAsString(exec, false, true, null, null);
        exec = ScriptingLogicsModule.transformFormulaText(exec, SQLSession.getParamName("$1"));

        List<String> tempTables = new ArrayList<>();
        try {
            ImMap<String, ParseInterface> paramObjects = replaceParams(context, conn, syntax, owner, tempTables);

            ParsedStatement parsed = SQLCommand.preparseStatement(exec, paramObjects, syntax).parseStatement(conn, syntax);

            try {
                SQLSession.ParamNum paramNum = new SQLSession.ParamNum();
                for (String param : parsed.preparedParams)
                    paramObjects.get(param).writeParam(parsed.statement, paramNum, syntax);

                boolean isResultSet = (boolean) Executors.newSingleThreadExecutor().submit((Callable) parsed.statement::execute).get();

                int resultNum = 0;
                while (true) {
                    if(resultNum >= targetPropList.size())
                        break;
                    LP targetProp = targetPropList.get(resultNum++);

                    if (isResultSet) {
                        writeResult(targetProp, JDBCTable.serialize(parsed.statement.getResultSet()), TableClass.extension, context, ExternalUtils.resultCharset.toString());
                    } else {
                        int updateCount = parsed.statement.getUpdateCount();
                        if (updateCount == -1)
                            break;

                        targetProp.change(updateCount, context);
                    }
                    isResultSet = parsed.statement.getMoreResults();
                }
            } catch (InterruptedException e) {
                parsed.statement.cancel();
                throw Throwables.propagate(e);
            } finally {
                parsed.statement.close();
            }
        } finally {
            for(String table : tempTables)
                SQLSession.dropTemporaryTableFromDB(conn, syntax, table, owner);
        }
    }
}