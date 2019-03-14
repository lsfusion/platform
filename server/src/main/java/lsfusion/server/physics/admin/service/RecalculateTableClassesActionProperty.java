package lsfusion.server.physics.admin.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.DataObject;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.base.context.ThreadLocalContext.localize;

public class RecalculateTableClassesActionProperty extends ScriptingActionProperty {

    private final ClassPropertyInterface tableInterface;

    public RecalculateTableClassesActionProperty(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject tableObject = context.getDataKeyValue(tableInterface);
        boolean disableClasses = context.getBL().reflectionLM.disableClassesTable.read(context, tableObject) != null;
        if (!disableClasses) {
            final String tableName = (String) context.getBL().reflectionLM.sidTable.read(context, tableObject);

            ServiceDBActionProperty.run(context, new RunService() {
                public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                    context.getDbManager().recalculateTableClasses(session, tableName.trim(), isolatedTransaction);
                }
            });

            context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", localize("{logics.recalculating.data.classes}"))), localize("{logics.recalculating.data.classes}")));
        }
    }
}
