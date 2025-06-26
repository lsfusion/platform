package lsfusion.server.physics.admin.service.action;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropColumnsAction extends InternalAction {

    public DropColumnsAction(ReflectionLogicsModule LM) {
        super(LM);
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Map<String, List<String>> tableColumns = readTableColumns(context);
        for(Map.Entry<String, List<String>> entry : tableColumns.entrySet()) {
            try {
                context.getDbManager().dropColumns(context.getSession().sql, entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                ServerLoggers.sqlLogger.error("Error dropping columns", e);
            }
        }
    }

    private Map<String, List<String>> readTableColumns(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Map<String, List<String>> tableColumns = new HashMap<>();

        KeyExpr dropColumnExpr = new KeyExpr("dropColumn");
        QueryBuilder<String, Object> query = new QueryBuilder<>(MapFact.singletonRev("dropColumn", dropColumnExpr));

        BusinessLogics BL = context.getBL();
        query.addProperty("sidColumn", BL.reflectionLM.sidDropColumn.getExpr(dropColumnExpr));
        query.addProperty("sidTable", BL.reflectionLM.sidTableDropColumn.getExpr(dropColumnExpr));
        query.and(BL.reflectionLM.sidDropColumn.getExpr(dropColumnExpr).getWhere());
        query.and(BL.reflectionLM.sidTableDropColumn.getExpr(dropColumnExpr).getWhere());

        ImOrderMap<ImMap<String, Object>, ImMap<Object, Object>> result = query.execute(context);
        for (int i = 0; i < result.size(); i++) {
            ImMap<Object, Object> valueEntry = result.getValue(i);

            String sidColumn = (String) valueEntry.get("sidColumn");
            String sidTable = (String) valueEntry.get("sidTable");
            List<String> columns = tableColumns.getOrDefault(sidTable, new ArrayList<>());
            columns.add(sidColumn);
            tableColumns.put(sidTable, columns);
        }
        return tableColumns;
    }
}