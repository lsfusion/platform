package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.nvl;

// compiles a customization code as it is compiled where it is used, running nothing of it, and writes what the compilation
// failed with to extendCodeError[] (NULL if it compiles): a code that does not compile is not saved - where it is used,
// such a code is skipped. Whatever the compilation throws is what it failed with : an error of the code, or a check of
// what the code makes (the objects of a form, the copy of the navigator). A code runs as a script does, so one of a user
// who may not evaluate a script (UserController.canEvaluate) is not compiled, and not saved either. A code compiled only
// with the whole logics, as the server starts, is not compiled here - only the right of its user is checked -, and an
// error of it stops the start
public abstract class CompileExtendCodeAction extends InternalAction {
    protected CompileExtendCodeAction(SystemEventsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String code = (String) context.getKeyObject(getOrderInterfaces().last());
        String error = null;
        if (code != null) {
            if (!context.getSession().user.canEvaluate())
                error = ThreadLocalContext.localize("{action.form.customize.forbidden}");
            else {
                try {
                    error = compile(context, code);
                } catch (RuntimeException e) {
                    error = nvl(e.getMessage(), e.toString());
                }
            }
        }
        try {
            findProperty("extendCodeError[]").change(error, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    // what the compilation failed with, unless it throws it
    protected abstract String compile(ExecutionContext<ClassPropertyInterface> context, String code) throws SQLException, SQLHandledException;
}
