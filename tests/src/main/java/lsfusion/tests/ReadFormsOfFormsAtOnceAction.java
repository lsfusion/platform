package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.AutoFinalFormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// a form built from another one (FORMS) read during the parse at a module's version - as DESIGN looks a component up -
// beside a FORMS statement extending it, which holds the form as it writes the form's components. A gets its property
// after the copy, so B maps it only as it is read, waiting for B : the read must hold none of B's components meanwhile.
// What the test saw goes into ffReadFailure
public class ReadFormsOfFormsAtOnceAction extends AtOnceAction {

    public ReadFormsOfFormsAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            BaseLogicsModule baseLM = context.getBL().LM;
            Version version = baseLM.getVersion();
            AutoFinalFormEntity a = new AutoFinalFormEntity(LocalizedString.create("A"), baseLM);
            ObjectEntity item = a.addSingleGroupObject(findClass("FfItem"));
            AutoFinalFormEntity b = new AutoFinalFormEntity(LocalizedString.create("B"), baseLM);
            FinalizeFormsOfFormsAction.addForm(b, a, version);
            a.addPropertyDraw(findProperty("ffNote[FfItem]"), SetFact.singletonOrder(item));

            String failure = runWhileHeld(context, b, "form B's design was read at a module's version without waiting for form B, which maps what B copies",
                    () -> b.view.getComponentBySID("ffNoSuchComponent", version),
                    thread -> holds(thread, b.view.components) ? "reading form B's design at a module's version waited for form B"
                            + " holding B's components, which a FORMS statement extending B writes as it holds B" : null);

            findProperty("ffReadFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
