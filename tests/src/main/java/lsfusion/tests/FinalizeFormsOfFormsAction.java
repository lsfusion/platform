package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NFImpl;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.event.FormChangeEvent;
import lsfusion.server.logics.form.struct.AutoFinalFormEntity;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.lang.reflect.Field;
import java.sql.SQLException;

// forms built from forms finalized at once, as the start's chunks finalize them. The test builds three forms as a parse
// does - B from A, C from B - and then gives A a property and an event after its change (ON CHANGE PROPERTY AFTER), as a
// module parsed later would, so that B maps them only as it is read. It begins B's finalization - its groups - and holds
// form B, as that finalization does, and finalizes C in another thread : C has to wait for B, to map A's property into
// it, and it must hold none of B's collections meanwhile - B's finalization takes them as it holds B, and the two would
// wait for each other. A started form is finalized already, so the three are built at runtime. What the test saw goes
// into ffFailure
public class FinalizeFormsOfFormsAction extends AtOnceAction {

    public FinalizeFormsOfFormsAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            BaseLogicsModule baseLM = context.getBL().LM;
            Version version = baseLM.getVersion();
            AutoFinalFormEntity a = new AutoFinalFormEntity(LocalizedString.create("A"), baseLM);
            ObjectEntity item = a.addSingleGroupObject(findClass("FfItem"));
            a.addPropertyDraw(findProperty("ffName[FfItem]"), SetFact.singletonOrder(item));
            AutoFinalFormEntity b = new AutoFinalFormEntity(LocalizedString.create("B"), baseLM);
            addForm(b, a, version);
            AutoFinalFormEntity c = new AutoFinalFormEntity(LocalizedString.create("C"), baseLM);
            addForm(c, b, version);
            PropertyDrawEntity<?, ?> note = a.addPropertyDraw(findProperty("ffNote[FfItem]"), SetFact.singletonOrder(item));
            a.addActionsOnEvent(new FormChangeEvent(note, false), version, new ActionObjectEntity<>(findAction("ffNoted[]").action, MapFact.EMPTYREV()));
            b.getGroupsIt(); // what B's own finalization does first

            String failure = runWhileHeld(context, b, "form C was finalized without waiting for form B, which it is built from",
                    c::finalizeAroundInit, thread -> {
                        String held = heldCollection(thread, b);
                        if (held == null)
                            held = heldCollection(thread, b.view);
                        return held != null ? "the finalization of form C waited for form B holding B's " + held + ", which B's own finalization takes" : null;
                    });

            findProperty("ffFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    // FORMS addForm in form, as the parse does it
    private static void addForm(FormEntity form, FormEntity addForm, Version version) {
        ObjectMapping mapping = new ObjectMapping(form, addForm, null, null, null, null, null, false, version);
        ((DefaultFormView) mapping.get(addForm.view)).addForm(addForm.view, mapping, version);
    }

    // the collection of the owner the thread holds, if any
    private static String heldCollection(Thread thread, Object owner) {
        try {
            for (Class<?> cls = owner.getClass(); cls != null; cls = cls.getSuperclass())
                for (Field field : cls.getDeclaredFields())
                    if (!field.getType().isPrimitive()) {
                        field.setAccessible(true);
                        Object collection = field.get(owner);
                        if (collection instanceof NFImpl && holds(thread, collection))
                            return field.getName();
                    }
            return null;
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
}
