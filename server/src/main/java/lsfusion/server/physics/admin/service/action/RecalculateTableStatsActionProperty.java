package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.physics.admin.service.RunService;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class RecalculateTableStatsActionProperty extends ScriptingAction {

    private final ClassPropertyInterface tableInterface;

    public RecalculateTableStatsActionProperty(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject tableObject = context.getDataKeyValue(tableInterface);
        final String tableName = (String) context.getBL().reflectionLM.sidTable.read(context, tableObject);
        final Set<String> disableStatsTableColumnSet = context.getDbManager().getDisableStatsTableColumnSet();
        boolean disableStats = context.getBL().reflectionLM.disableStatsTable.read(context, tableObject) != null;
        if (!disableStats) {
            ServiceDBActionProperty.run(context, new RunService() {
                        public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                            try (ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
                                context.getBL().LM.tableFactory.getImplementTablesMap().get(tableName).recalculateStat(context.getBL().reflectionLM,
                                        disableStatsTableColumnSet, newContext.getSession());
                                newContext.apply();
                            }
                        }
                    }
            );
            context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", localize("{logics.recalculation.stats}"))), localize("{logics.recalculation.stats}")));
        }
    }
}