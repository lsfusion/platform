package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.identity.IDGenerator;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.struct.AutoFinalFormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// a form built from another one (FORMS) copies what it maps of the other one as it is first asked for it, and two
// threads can ask for one thing at once : the second has to wait for the copy the first is making, not get it half made.
// The test builds B from A at runtime and gives A a property after the copy, so that B maps it only when asked. One
// thread asks for it and is held as the copy takes its id - past the copy's registration in the mapping -, another asks
// for it meanwhile : it has to wait for form B, which the first holds, and then get the same copy. What the test saw goes
// into ffMapFailure
public class MapFormsOfFormsAtOnceAction extends AtOnceAction {

    public MapFormsOfFormsAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
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
            ObjectMapping mapping = new ObjectMapping(b, a, null, null, null, null, null, false, version);
            ((DefaultFormView) mapping.get(a.view)).addForm(a.view, mapping, version);
            PropertyDrawEntity<?, ?> note = a.addPropertyDraw(findProperty("ffNote[FfItem]"), SetFact.singletonOrder(item));

            Hold hold = new Hold();
            IDGenerator ids = b.genID;
            b.genID = () -> hold.call(ids::id);
            AtomicReference<PropertyDrawEntity<?, ?>> first = new AtomicReference<>();
            AtomicReference<PropertyDrawEntity<?, ?>> second = new AtomicReference<>();
            AtomicBoolean halfMade = new AtomicBoolean();
            String failure = runAtOnce(context, hold, b, "two threads made copies of one property at once",
                    () -> first.set(note.get(mapping)),
                    () -> {
                        second.set(note.get(mapping));
                        halfMade.set(second.get().getInheritedProperty() == null); // set by the copy's constructor past its id
                    });
            if (failure == null && halfMade.get())
                failure = "a thread asking form B's mapping for A's property got its copy half made, while another thread was still making it";
            else if (failure == null && first.get() != second.get())
                failure = "two threads asking form B's mapping for A's property got two copies";

            findProperty("ffMapFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
