package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.AutoFinalFormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.lang.reflect.Field;
import java.sql.SQLException;

// two threads binding keys to one property of a form at once, as two modules parsed at once that bind keys to one
// property do. The property's bindings are a collection of its own - a private field, reached by reflection -, written
// on its monitor, so the test holds it, as a first binding would, and a second one has to wait. A form the server started
// with is finished and takes no more bindings, so the test builds one. What the test saw goes into kbFailure
public class BindKeysAtOnceAction extends AtOnceAction {

    public BindKeysAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            BaseLogicsModule baseLM = context.getBL().LM;
            Version version = baseLM.getVersion();
            AutoFinalFormEntity form = new AutoFinalFormEntity(LocalizedString.create("Key bindings"), baseLM);
            PropertyDrawEntity<?, ?> property = form.addPropertyDraw(findProperty("kbName[]"), SetFact.EMPTYORDER());
            Field field = PropertyDrawEntity.class.getDeclaredField("keyBindings");
            field.setAccessible(true);
            NFMap<KeyStroke, String> bindings = (NFMap<KeyStroke, String>) field.get(property);

            property.setKeyAction(KeyStroke.getKeyStroke("F5"), "kbFirst", version);
            String failure = runWhileHeld(context, bindings, "a thread bound a key to the property while another one held its bindings",
                    () -> property.setKeyAction(KeyStroke.getKeyStroke("F6"), "kbSecond", version));
            if (failure == null && !("kbFirst".equals(bindings.getNFValue(KeyStroke.getKeyStroke("F5"), version)) &&
                                     "kbSecond".equals(bindings.getNFValue(KeyStroke.getKeyStroke("F6"), version))))
                failure = "the property kept the bindings " + bindings.getNFCopy(version);

            findProperty("kbFailure[]").change(failure, context);
        } catch (NoSuchFieldException | IllegalAccessException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
