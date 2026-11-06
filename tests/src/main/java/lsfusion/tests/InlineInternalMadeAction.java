package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.reflect.Field;
import java.sql.SQLException;

// whether the inline INTERNAL ecrInlineTarget has an instance of its code yet : no API says it, so it is read off the action's field
public class InlineInternalMadeAction extends InternalAction {

    public InlineInternalMadeAction(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Action<?> target = findAction("ecrInlineTarget[]").action;
            Field code = target.getClass().getDeclaredField("code");
            code.setAccessible(true);
            findProperty("ecrInlineMade[]").change(code.get(target) != null ? Boolean.TRUE : null, context);
        } catch (ScriptingErrorLog.SemanticErrorException | ReflectiveOperationException e) {
            throw Throwables.propagate(e);
        }
    }
}
