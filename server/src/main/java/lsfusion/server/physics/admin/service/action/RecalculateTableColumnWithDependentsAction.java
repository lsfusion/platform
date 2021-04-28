package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
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

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class RecalculateTableColumnWithDependentsAction extends InternalAction {
    private final ClassPropertyInterface tableColumnInterface;

    public RecalculateTableColumnWithDependentsAction(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableColumnInterface = i.next();
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject tableColumnObject = context.getDataKeyValue(tableColumnInterface);
        final ObjectValue propertyObject = context.getBL().reflectionLM.propertyTableColumn.readClasses(context, tableColumnObject);
        final String propertyCanonicalName = (String) context.getBL().reflectionLM.canonicalNameProperty.read(context, propertyObject);
        boolean disableAggregations = context.getBL().reflectionLM.disableAggregationsTableColumn.read(context, tableColumnObject) != null;
        if (!disableAggregations) {
            ServiceDBAction.run(context, (session, isolatedTransaction) -> context.getDbManager().recalculateAggregationWithDependenciesTableColumn(session, context.stack, propertyCanonicalName.trim(), isolatedTransaction, false));

            context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", localize("{logics.recalculation.aggregations}"))), localize("{logics.recalculation.aggregations}")));
        }
    }
}