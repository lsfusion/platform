package lsfusion.server.physics.admin.service.action;

import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;

import java.sql.SQLException;

public class DropColumnActionProperty extends ScriptingAction {

    public DropColumnActionProperty(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        BusinessLogics BL = context.getBL();
        DataObject dropColumnObject = context.getSingleDataKeyValue();
        String columnName = (String) BL.reflectionLM.sidDropColumn.getOld().read(context, dropColumnObject);
        String tableName = (String) BL.reflectionLM.sidTableDropColumn.getOld().read(context, dropColumnObject);
        try {
            context.getDbManager().dropColumn(tableName, columnName);
        } catch (SQLException e) {
            ServerLoggers.sqlLogger.error("Ошибка при удалении колонки", e);
        }
    }
}
