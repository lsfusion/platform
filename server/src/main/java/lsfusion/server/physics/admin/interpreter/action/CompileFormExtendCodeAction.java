package lsfusion.server.physics.admin.interpreter.action;

import lsfusion.server.language.EvalScriptingLogicsModule;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;

// a form's code, compiled as the form compiles it when it opens (FormEntity.getCustomizedForm) in a connection the given
// application's code customizes - what the code uses of that one is seen
public final class CompileFormExtendCodeAction extends CompileExtendCodeAction {
    public CompileFormExtendCodeAction(SystemEventsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected String compile(ExecutionContext<ClassPropertyInterface> context, String code) {
        BusinessLogics BL = context.getBL();
        FormEntity form = BL.findForm((String) getParamValue(0, context).getValue());

        // a connection whose application's code does not compile goes on without it (RemoteConnection.initCustomize)
        String applicationCode = (String) getParamValue(1, context).getValue();
        EvalScriptingLogicsModule applicationLM = null;
        if (applicationCode != null) {
            try {
                applicationLM = RemoteConnection.evaluateCustomizeCode(BL, applicationCode);
            } catch (RuntimeException e) {
            }
        }
        try {
            form.getCustomizedForm(BL, applicationLM, code);
            return null;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            return e.getMessage();
        }
    }
}
