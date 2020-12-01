package lsfusion.server.physics.admin.monitor.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

import static lsfusion.base.SystemUtils.getRevision;

public class GetCurrentRevisionAction extends InternalAction {

    public GetCurrentRevisionAction(SystemEventsLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        try {
            context.getBL().systemEventsLM.currentRevision.change(getRevision(SystemProperties.inDevMode), context);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}