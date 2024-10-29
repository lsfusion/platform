package lsfusion.server.physics.admin.service.action;

import lsfusion.base.Result;
import lsfusion.interop.action.MessageClientType;
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

import static lsfusion.base.BaseUtils.isEmpty;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class CheckTableClassesAction extends InternalAction {

    private final ClassPropertyInterface tableInterface;

    public CheckTableClassesAction(ReflectionLogicsModule LM, ValueClass... classes) {
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
            final Result<String> message = new Result<>();
            ServiceDBAction.run(context, DBManager.CHECK_CLASSES_TIL, (session, isolatedTransaction) -> message.set(context.getDbManager().checkTableClasses(session, tableName.trim(), isolatedTransaction)));

            boolean noErrors = isEmpty(message.result);

            context.message(localize(LocalizedString.createFormatted(noErrors ? "{logics.check.completed}" : "{logics.check.failed}",
                    localize("{logics.checking.data.classes}"))) + (noErrors ? "" : ("\n\n" + message.result)),
                    localize("{logics.checking.data.classes}"), noErrors ? MessageClientType.SUCCESS : MessageClientType.ERROR);
        }
    }
}
