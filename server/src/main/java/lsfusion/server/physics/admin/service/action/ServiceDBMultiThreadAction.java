package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.RunService;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class ServiceDBMultiThreadAction extends InternalAction {
    private ClassPropertyInterface threadCountInterface;
    ServiceLogicsModule serviceLM;
    public ServiceDBMultiThreadAction(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        this.serviceLM = LM;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        threadCountInterface = i.next();
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        final ObjectValue threadCountObject = context.getKeyValue(threadCountInterface);

        serviceLM.recalculateMultiThreadAction.execute(context, threadCountObject);

        serviceLM.recalculateClassesMultiThreadAction.execute(context, threadCountObject);

        context.getDbManager().analyzeDB(context.getSession().sql);

        serviceLM.recalculateFollowsMultiThreadAction.execute(context, threadCountObject);

        serviceLM.recalculateStatsMultiThreadAction.execute(context, threadCountObject);

        context.messageSuccess(localize("{logics.service.db.completed}"), localize("{logics.service.db}"));
    }
}