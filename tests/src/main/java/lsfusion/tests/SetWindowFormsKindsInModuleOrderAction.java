package lsfusion.tests;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.navigator.window.FormsWindow;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import com.google.common.base.Throwables;

import java.sql.SQLException;

// two modules that do not require each other extending FORMS windows - declared by a module both require, drawing one
// form at a time -, the later one in the module order writing first, as the parse threads' timing can have it. One
// window : the later module turns it TABBED, the earlier one says CLOSE 5 - checked against the window as the earlier
// module's requirements left it, one form at a time, not refused by the other one's TABBED -, and the window keeps the
// TABBED beside the delay. Another : the later module turns it TABBED, the earlier one NOTABBED, and it keeps TABBED.
// The windows are the test's own, out of the navigator. What the test saw goes into fkFailure
public class SetWindowFormsKindsInModuleOrderAction extends AtOnceAction {

    public SetWindowFormsKindsInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            LogicsModule[] modules = independentModules(context.getBL(), LM);

            FormsWindow window = new FormsWindow("WindowFormsKinds.fkWindow", LocalizedString.create("fk"), true, 0, context.getBL().LM.getVersion());
            window.setFormsKind(false, null, modules[1].getVersion());
            window.setFormsKind(null, 5, modules[0].getVersion());
            String failure = null;
            if (!window.isNFSingleForm(modules[0].getVersion()))
                failure = modules[0].getName() + "'s CLOSE was checked against the TABBED of " + modules[1].getName() + ", which it does not require";
            if (failure == null && !(!window.isSingleForm() && window.getFormCloseDelay() == 5))
                failure = "the window kept " + (window.isSingleForm() ? "NOTABBED" : "TABBED") + " with the delay " + window.getFormCloseDelay()
                        + ", not " + modules[1].getName() + "'s TABBED beside " + modules[0].getName() + "'s delay";

            FormsWindow other = new FormsWindow("WindowFormsKinds.fkOther", LocalizedString.create("fk"), true, 0, context.getBL().LM.getVersion());
            other.setFormsKind(false, null, modules[1].getVersion());
            other.setFormsKind(true, null, modules[0].getVersion());
            if (failure == null && other.isSingleForm())
                failure = "the window kept NOTABBED of " + modules[0].getName() + ", written last, not TABBED of " + modules[1].getName() + ", later in the module order";

            findProperty("fkFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
