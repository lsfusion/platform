package lsfusion.erp.utils;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class ChangeAllDatesActionProperty extends ScriptingActionProperty {

    public ChangeAllDatesActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        changeAllDates(context);
    }

    private void changeAllDates(ExecutionContext context) throws SQLException, SQLHandledException {

        DataSession session = context.createSession();
        try {

            session.sql.pushNoReadOnly(session.sql.getConnection().sql);

            Integer seconds = (Integer) findProperty("secondsChangeAllDates").read(session);
            if (seconds != null) {

                KeyExpr propertyExpr = new KeyExpr("property");

                ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "property", propertyExpr);
                QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
                query.addProperty("dbName", findProperty("dbName[Property]").getExpr(propertyExpr));
                query.addProperty("return", findProperty("return[Property]").getExpr(propertyExpr));
                query.addProperty("tableSID", findProperty("tableSID[Property]").getExpr(propertyExpr));
                query.and(findProperty("dbName[Property]").getExpr(propertyExpr).getWhere());
                query.and(findProperty("return[Property]").getExpr(propertyExpr).getWhere());
                query.and(findProperty("tableSID[Property]").getExpr(propertyExpr).getWhere());

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(session);
                for (ImMap<Object, Object> valueEntry : result.values()) {
                    String column = (String) valueEntry.get("dbName");
                    String table = (String) valueEntry.get("tableSID");
                    String returnProperty = (String) valueEntry.get("return");
                    if (!table.isEmpty() && !column.isEmpty() && (returnProperty.equals("TIME") || returnProperty.equals("DATETIME"))) {
                        ServerLoggers.systemLogger.info(String.format("Changing date: column %s, table %s", column, table));
                        session.sql.executeDDL(String.format("UPDATE %s SET %s = %s + %s*INTERVAL '1 second'", table, column, column, seconds));
                    }
                }

                KeyExpr tableKeyExpr = new KeyExpr("tableKey");

                ImRevMap<Object, KeyExpr> tableKeyKeys = MapFact.singletonRev((Object) "tableKey", tableKeyExpr);
                QueryBuilder<Object, Object> tableKeyQuery = new QueryBuilder<>(tableKeyKeys);
                tableKeyQuery.addProperty("classSID", findProperty("classSID[TableKey]").getExpr(tableKeyExpr));
                tableKeyQuery.addProperty("name", findProperty("name[TableKey]").getExpr(tableKeyExpr));
                tableKeyQuery.addProperty("sidTable", findProperty("sidTable[TableKey]").getExpr(tableKeyExpr));
                tableKeyQuery.and(findProperty("classSID[TableKey]").getExpr(tableKeyExpr).getWhere());
                tableKeyQuery.and(findProperty("name[TableKey]").getExpr(tableKeyExpr).getWhere());

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> tableKeyResult = tableKeyQuery.execute(session);
                for (ImMap<Object, Object> valueEntry : tableKeyResult.values()) {
                    String classSID = (String) valueEntry.get("classSID");
                    String key = (String) valueEntry.get("name");
                    String table = (String) valueEntry.get("sidTable");
                    if (!key.isEmpty() && !table.isEmpty() && (classSID.equals("TIME") || classSID.equals("DATETIME"))) {
                        ServerLoggers.systemLogger.info(String.format("Changing date: key %s, table %s", key, table));
                        session.sql.executeDDL(String.format("UPDATE %s SET %s = %s + %s*INTERVAL '1 second'", table, key, key, seconds));
                    }
                }

                session.apply(context);
            }

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();
        } finally {
            session.sql.popNoReadOnly(session.sql.getConnection().sql);
        }
    }
}