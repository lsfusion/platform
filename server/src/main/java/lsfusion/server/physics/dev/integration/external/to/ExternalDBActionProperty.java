package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.logics.classes.DynamicFormatFileClass;
import lsfusion.server.data.*;
import lsfusion.server.data.sql.DefaultSQLSyntax;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.AbstractParseInterface;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.action.flow.FlowResult;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExternalDBActionProperty extends ExternalActionProperty {

    private PropertyInterface connectionString;
    private PropertyInterface exec;

    public ExternalDBActionProperty(ImList<Type> params, ImList<LCP> targetPropList) {
        super(2, params, targetPropList); // строка подключения, команда + параметры

        ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();
        connectionString = orderInterfaces.get(0);
        exec = orderInterfaces.get(1);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        String replacedParams = replaceParams(context, getTransformedText(context, connectionString));
        List<Object> results = readJDBC(context.getKeys(), replacedParams, getTransformedText(context, exec));

        for (int i = 0; i < targetPropList.size(); i++)
            targetPropList.get(i).change(results.get(i), context);

        return FlowResult.FINISH;
    }

    private List<Object> readJDBC(ImMap<PropertyInterface, ? extends ObjectValue> params, String connectionString, String exec) throws SQLException, SQLHandledException {
        SQLSyntax syntax = DefaultSQLSyntax.getSyntax(connectionString);
        OperationOwner owner = OperationOwner.unknown;

        Connection conn = DriverManager.getConnection(connectionString);
        List<String> tempTables = new ArrayList<>();

        try {
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

                boolean isResultSet = parsed.statement.execute();

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
            } finally {
                parsed.statement.close();
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            for(String table : tempTables)
                SQLSession.dropTemporaryTableFromDB(conn, syntax, table, owner);
            if (conn != null)
                conn.close();
        }
    }
}