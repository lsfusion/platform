package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.navigator.window.Orientation;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// two modules that do not require each other giving windows their CUSTOM, the later one in the module order writing
// first, as the parse threads' timing can have it : a window keeps the later module's literal beside the earlier one's
// property, and a React component the earlier module gives another window the later one gave a property is not refused
// by the earlier one's statement, which checks only what it gives, but by the final window's check. The windows are the
// test's own, out of the navigator. What the test saw goes into wcOrderFailure
public class SetWindowCustomsInModuleOrderAction extends AtOnceAction {

    public SetWindowCustomsInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            LogicsModule[] modules = independentModules(context.getBL(), LM);
            Property<?> custom = findProperty("wcCustom[]").property;

            NavigatorWindow window = new NavigatorWindow(Orientation.VERTICAL.asToolbarOrientation(), "WindowCustoms.wcWindow", LocalizedString.create("wc"));
            window.setCustoms("<div>later</div>", null, modules[1].getVersion());
            window.setCustoms("<div>earlier</div>", custom, modules[0].getVersion());
            String failure = null;
            if (!("<div>later</div>".equals(window.getCustom()) && window.getPropertyCustom() == custom))
                failure = "the window kept the CUSTOM " + window.getCustom() + " computed by " + window.getPropertyCustom()
                        + ", not " + modules[1].getName() + "'s literal beside " + modules[0].getName() + "'s property";

            NavigatorWindow other = new NavigatorWindow(Orientation.VERTICAL.asToolbarOrientation(), "WindowCustoms.wcOther", LocalizedString.create("wc"));
            other.setCustoms(null, custom, modules[1].getVersion());
            String refused = other.setCustoms("WcComponent", null, modules[0].getVersion());
            if (failure == null && refused != null)
                failure = modules[0].getName() + "'s React component was refused by the property of " + modules[1].getName() + ", another statement's";
            if (failure == null) {
                try {
                    other.checkCustoms();
                    failure = "the final window with a React component and a property was not refused";
                } catch (RuntimeException e) {
                    if (!e.getMessage().contains("WcComponent"))
                        failure = "the final window was refused with " + e.getMessage();
                }
            }

            findProperty("wcOrderFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
