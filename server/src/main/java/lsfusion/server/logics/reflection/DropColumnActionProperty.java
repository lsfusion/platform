package lsfusion.server.logics.reflection;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ReflectionLogicsModule;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class DropColumnActionProperty extends ScriptingActionProperty {

    LAP delete;

    public DropColumnActionProperty(ReflectionLogicsModule LM) {
        super(LM, new ValueClass[]{LM.dropColumn});
        delete = LM.getDeleteAction(LM.dropColumn, true);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        BusinessLogics BL = context.getBL();
        DataObject dropColumnObject = context.getSingleDataKeyValue();
        String columnName = (String) BL.reflectionLM.sidDropColumn.getOld().read(context, dropColumnObject);
        String tableName = (String) BL.reflectionLM.sidTableDropColumn.getOld().read(context, dropColumnObject);
        try {
            context.getDbManager().dropColumn(tableName, columnName);
        } catch (SQLException e) {
            context.requestUserInteraction(new MessageClientAction(e.getMessage(), "Ошибка при удалении колонки"));
        }
        delete.execute(context, dropColumnObject);
    }
}
