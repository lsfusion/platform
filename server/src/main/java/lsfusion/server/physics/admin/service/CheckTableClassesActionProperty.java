package lsfusion.server.physics.admin.service;

import lsfusion.base.Result;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.base.thread.ThreadLocalContext.localize;

public class CheckTableClassesActionProperty extends ScriptingAction {

    private final ClassPropertyInterface tableInterface;

    public CheckTableClassesActionProperty(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject tableObject = context.getDataKeyValue(tableInterface);
        final String tableName = (String) context.getBL().reflectionLM.sidTable.read(context, tableObject);

        final Result<String> message = new Result<>();
        ServiceDBActionProperty.run(context, new RunService() {
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                message.set(context.getDbManager().checkTableClasses(session, tableName.trim(), isolatedTransaction));
            }
        });

        context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.check.completed}", localize("{logics.checking.data.classes}"))) + '\n' + '\n' + message.result, localize("{logics.checking.data.classes}"), true));
    }
}
