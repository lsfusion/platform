package lsfusion.server.physics.dev.migration.action;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.builder.QueryBuilder;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeAllDatesActionProperty extends ScriptingAction {

    public ChangeAllDatesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        changeAllDates(context);
    }

    private void changeAllDates(ExecutionContext context) throws SQLException, SQLHandledException {

        SQLSession sql = context.getSession().sql;
        try {
            sql.pushNoReadOnly();
            
            try(ExecutionContext.NewSession newContext = context.newSession()) {
    
                Integer seconds = (Integer) findProperty("secondsChangeAllDates").read(newContext);
                if (seconds != null) {
    
                    Map<String, List<String>> tableColumnsMap = new HashMap<>();
    
                    KeyExpr propertyExpr = new KeyExpr("property");
    
                    ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "property", propertyExpr);
                    QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
                    Expr dbNameExpr = findProperty("dbName[Property]").getExpr(propertyExpr);
                    Expr returnExpr = findProperty("return[Property]").getExpr(propertyExpr);
                    Expr tableSIDExpr = findProperty("tableSID[Property]").getExpr(propertyExpr);
                    query.addProperty("dbName", dbNameExpr);
                    query.addProperty("return", returnExpr);
                    query.addProperty("tableSID", tableSIDExpr);
                    query.and(dbNameExpr.getWhere());
                    query.and(returnExpr.getWhere());
                    query.and(tableSIDExpr.getWhere());
    
                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(newContext);
                    for (ImMap<Object, Object> valueEntry : result.values()) {
                        String column = (String) valueEntry.get("dbName");
                        String table = (String) valueEntry.get("tableSID");
                        String returnProperty = (String) valueEntry.get("return");
                        if (!table.isEmpty() && !column.isEmpty() && (returnProperty.equals("DATE") || returnProperty.equals("TIME") || returnProperty.equals("DATETIME"))) {
                            List<String> columns = tableColumnsMap.get(table);
                            if (columns == null) columns = new ArrayList<>();
                            columns.add(column);
                            tableColumnsMap.put(table, columns);
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
    
                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> tableKeyResult = tableKeyQuery.execute(newContext);
                    for (ImMap<Object, Object> valueEntry : tableKeyResult.values()) {
                        String classSID = (String) valueEntry.get("classSID");
                        String key = (String) valueEntry.get("name");
                        String table = (String) valueEntry.get("sidTable");
                        if (!key.isEmpty() && !table.isEmpty() && (classSID.equals("TIME") || classSID.equals("DATETIME"))) {
                            List<String> columns = tableColumnsMap.get(table);
                            if (columns == null) columns = new ArrayList<>();
                            columns.add(key);
                            tableColumnsMap.put(table, columns);
                        }
                    }
    
                    int count = 1;
                    for (Map.Entry<String, List<String>> entry : tableColumnsMap.entrySet()) {
                        String table = entry.getKey();
                        StringBuilder columns = new StringBuilder();
                        StringBuilder logColumns = new StringBuilder();
                        for (String column : entry.getValue()) {
                            columns.append(String.format("%s%s = %s + %s*INTERVAL '1 second'", columns.length() == 0 ? "" : ", ", column, column, seconds));
                            logColumns.append(String.format("%s%s", (logColumns.length() == 0) ? "" : ", ", column));
                        }
                        ServerLoggers.systemLogger.info(String.format("Changing dates %s/%s: table %s, columns %s", count, tableColumnsMap.size(), table, logColumns.toString()));
                        sql.executeDDL(String.format("UPDATE %s SET %s", table, columns.toString()));
                        count++;
                    }

                    newContext.apply();
                }    
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();
        } finally {
            sql.popNoReadOnly();
        }
    }
}