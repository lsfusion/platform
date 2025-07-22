package lsfusion.server.physics.admin.service.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class RemoveSlaveAction extends InternalAction {

    private final ClassPropertyInterface hostInterface;

    public RemoveSlaveAction(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        hostInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            context.getDbManager().removeSlave(context.getDataKeyValue(hostInterface), context.getSession());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
