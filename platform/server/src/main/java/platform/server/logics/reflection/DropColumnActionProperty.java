package platform.server.logics.reflection;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ReflectionLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class DropColumnActionProperty extends ScriptingActionProperty {
    public DropColumnActionProperty(ReflectionLogicsModule LM) {
        super(LM, new ValueClass[]{LM.getClassByName("DropColumn")});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        BusinessLogics BL = context.getBL();
        DataObject dropColumnObject = context.getSingleKeyValue();
        String columnName = (String) BL.reflectionLM.sidDropColumn.getOld().read(context, dropColumnObject);
        String tableName = (String) BL.reflectionLM.sidTableDropColumn.getOld().read(context, dropColumnObject);
        try {
            context.getDbManager().dropColumn(tableName, columnName);
        } catch (SQLException e) {
            context.requestUserInteraction(new MessageClientAction(e.getMessage(), "Ошибка при удалении колонки"));
        }
        LM.baseLM.delete.execute(context, dropColumnObject);
    }
}
