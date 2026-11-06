package lsfusion.server.physics.dev.integration.internal.to;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

// an INTERNAL with its code inline : the code is compiled with the module, so its errors are the module's, but the class it is compiled to
// gets its instance only when the action is first run - until then none of that code runs
public class InlineInternalAction extends InternalAction {

    private final Class<?> codeClass;
    private InternalAction code;

    public InlineInternalAction(ScriptingLogicsModule LM, Class<?> codeClass) {
        super(LM);
        this.codeClass = codeClass;
    }

    private synchronized InternalAction getCode() {
        if (code == null) {
            try {
                code = (InternalAction) codeClass.getConstructor(ScriptingLogicsModule.class).newInstance(LM);
            } catch (ReflectiveOperationException e) {
                throw Throwables.propagate(e);
            }
        }
        return code;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        getCode().commonExecuteInternalDelegate(context);
    }
}
