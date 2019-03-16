package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.TableOwner;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;
import lsfusion.server.physics.admin.service.RunService;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class PackTableActionProperty extends ScriptingAction {

    private final ClassPropertyInterface tableInterface;

    public PackTableActionProperty(ReflectionLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject tableObject = context.getDataKeyValue(tableInterface);
        final String tableName = (String) context.getBL().reflectionLM.sidTable.read(context, tableObject);

        ServiceDBActionProperty.run(context, new RunService() {
            public void run(final SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                DBManager.run(session, isolatedTransaction, new DBManager.RunService() {
                    @Override
                    public void run(SQLSession sql) throws SQLException {
                        ImplementTable table = context.getBL().LM.tableFactory.getImplementTablesMap().get(tableName);
                        session.packTable(table, OperationOwner.unknown, TableOwner.global);
                    }});

            }
        });
        context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.table.packing.completed}", localize("{logics.table.packing}"))), localize("{logics.table.packing}")));
    }
}