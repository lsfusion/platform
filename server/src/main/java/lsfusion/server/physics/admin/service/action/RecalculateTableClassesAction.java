package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class RecalculateTableClassesAction extends InternalAction {

    private final ClassPropertyInterface tableInterface;

    public RecalculateTableClassesAction(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableInterface = i.next();
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject tableObject = context.getDataKeyValue(tableInterface);
        boolean disableClasses = context.getBL().reflectionLM.disableClassesTable.read(context, tableObject) != null;
        if (!disableClasses) {
            final String tableName = (String) context.getBL().reflectionLM.sidTable.read(context, tableObject);

            ServiceDBAction.run(context, DBManager.RECALC_CLASSES_TIL, (session, isolatedTransaction) -> context.getDbManager().recalculateTableClasses(session, tableName.trim(), isolatedTransaction));

            context.messageSuccess(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", localize("{logics.recalculating.data.classes}"))), localize("{logics.recalculating.data.classes}"));
        }
    }
}
