package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.JoinAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.FormEventType;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.external.to.InternalClientAction;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

// what the ON INIT of FormsOfForms.ffLoading does last, into ffLastInit : INTERNAL CLIENT WAIT for an internal client
// call that waits, else the action
public class ReadLastInitAction extends InternalAction {

    public ReadLastInitAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Action<?> last = null;
            for (ActionObjectEntity<?> action : context.getBL().findForm("FormsOfForms.ffLoading").getEventActionsListIt(FormEventType.INIT))
                last = action.property;
            String lastInit = last != null ? last.toString() : null;
            if (last instanceof JoinAction && ((JoinAction<?>) last).action.action instanceof InternalClientAction && !last.hasFlow(ChangeFlowType.INTERNALASYNC))
                lastInit = "INTERNAL CLIENT WAIT";
            findProperty("ffLastInit[]").change(lastInit, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
