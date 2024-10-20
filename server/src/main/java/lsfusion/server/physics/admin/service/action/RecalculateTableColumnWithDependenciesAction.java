package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
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

public class RecalculateTableColumnWithDependenciesAction extends InternalAction {
    private final ClassPropertyInterface tableColumnInterface;

    public RecalculateTableColumnWithDependenciesAction(ReflectionLogicsModule LM, ValueClass... classes) {
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
            ServiceDBAction.run(context, DBManager.RECALC_MAT_TIL, (session, isolatedTransaction) -> context.getDbManager().recalculateMaterializationWithDependenciesTableColumn(session, context.stack, propertyCanonicalName.trim(), isolatedTransaction, true));

            context.messageSuccess(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", localize("{logics.recalculation.materializations}"))), localize("{logics.recalculation.materializations}"));
        }
    }
}
