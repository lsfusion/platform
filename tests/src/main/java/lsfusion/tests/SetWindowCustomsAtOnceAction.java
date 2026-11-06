package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.navigator.window.Orientation;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

// two threads giving one window its CUSTOM at once, as two modules parsed at once that extend one window do. The halves
// are written on the window's own monitor, so the test holds it, as a first statement would, and a second one has to
// wait. The window is one of the test's own, out of the navigator. What the test saw goes into wcFailure
public class SetWindowCustomsAtOnceAction extends AtOnceAction {

    public SetWindowCustomsAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Property<?> custom = findProperty("wcCustom[]").property;
            NavigatorWindow window = new NavigatorWindow(Orientation.VERTICAL.asToolbarOrientation(), "WindowCustoms.wcWindow", LocalizedString.create("wc"));
            String failure = runWhileHeld(context, window, "a thread gave the window a computed CUSTOM while another one held it",
                    () -> window.setPropertyCustom(custom));
            if (failure == null)
                failure = runWhileHeld(context, window, "a thread gave the window its CUSTOM while another one held it",
                        () -> window.setCustoms("<div>wc</div>", custom));
            if (failure == null && !("<div>wc</div>".equals(window.getCustom()) && window.getPropertyCustom() == custom))
                failure = "the window kept the CUSTOM " + window.getCustom() + " computed by " + window.getPropertyCustom();
            // a React component beside the property is refused before anything is written : what the check reads is held too
            AtomicReference<String> refused = new AtomicReference<>();
            if (failure == null)
                failure = runWhileHeld(context, window, "a thread checked the window's CUSTOM while another one held it",
                        () -> refused.set(window.setCustoms("WcComponent", null)));
            if (failure == null && !"WcComponent".equals(refused.get()))
                failure = "a React component beside a property was not refused, but " + refused.get();

            findProperty("wcFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
