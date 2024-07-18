package lsfusion.server.physics.admin.service.action;

import lsfusion.base.Result;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.base.BaseUtils.isEmpty;
import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class CheckTableColumnAction extends InternalAction {

    private final ClassPropertyInterface tableColumnInterface;

    public CheckTableColumnAction(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableColumnInterface = i.next();
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject tableColumnObject = context.getDataKeyValue(tableColumnInterface);
        final ObjectValue propertyObject = context.getBL().reflectionLM.propertyTableColumn.readClasses(context, tableColumnObject);
        final String propertyCanonicalName = (String) context.getBL().reflectionLM.canonicalNameProperty.read(context, propertyObject);
        boolean disableMaterializations = context.getBL().reflectionLM.disableMaterializationsTableColumn.read(context, tableColumnObject) != null;
        if (!disableMaterializations) {
            final Result<String> message = new Result<>();
            ServiceDBAction.run(context, (session, isolatedTransaction) ->
                    message.set(context.getDbManager().checkMaterializationTableColumn(session, propertyCanonicalName.trim())));

            boolean noErrors = isEmpty(message.result);

            context.message(localize(LocalizedString.createFormatted(noErrors ? "{logics.check.completed}" : "{logics.check.failed}",
                    localize("{logics.checking.materializations}"))) + (noErrors ? "" : ("\n\n" + message.result)),
                    localize("{logics.checking.materializations}"));
        }
    }
}
