package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import javax.swing.*;
import java.sql.SQLException;

// what F7 is bound to on the property `bound` of KeyBindings.kbBound and of the form built from it, kbBuilt, into
// kbBuiltBinding : null when the two are the same and not empty
public class ReadBuiltKeyBindingAction extends InternalAction {

    public ReadBuiltKeyBindingAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String bound = binding(context.getBL().findForm("KeyBindings.kbBound"));
            String built = binding(context.getBL().findForm("KeyBindings.kbBuilt"));
            findProperty("kbBuiltBinding[]").change(bound != null && bound.equals(built) ? null : "F7 is bound to " + bound + " on the form and to " + built + " on the form built from it", context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String binding(FormEntity form) {
        return form.getPropertyDraw("bound").getKeyBindings(form.getGlobalContext()).get(KeyStroke.getKeyStroke("F7"));
    }
}
