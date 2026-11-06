package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.NavigatorFolder;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

// two threads setting up one navigator element at once, as two modules parsed at once that set options of one element
// do. Each pair of options is written on the element's own monitor, so the test holds it, as a first statement would,
// and a second one has to wait. The element is one of the test's own, out of the navigator. What the test saw goes into
// noFailure
public class SetNavigatorOptionsAtOnceAction extends AtOnceAction {

    public SetNavigatorOptionsAtOnceAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            NavigatorElement element = new NavigatorFolder("NavigatorOptions.noFolder");
            NavigatorWindow window = (NavigatorWindow) context.getBL().findWindow("System.toolbar");
            Property<?> image = findProperty("noFailure[]").property;
            String failure = runWhileHeld(context, element, "a thread gave the element a window while another one held it",
                    () -> element.setWindow(window, true));
            if (failure == null)
                failure = runWhileHeld(context, element, "a thread gave the element an image while another one held it",
                        () -> element.setImage(AppServerImage.AUTO, image));
            if (failure == null)
                failure = runWhileHeld(context, element, "a thread gave the element a key while another one held it",
                        () -> element.setChangeKey("ctrl F12", true));
            if (failure == null)
                failure = runWhileHeld(context, element, "a thread gave the element a mouse binding while another one held it",
                        () -> element.setChangeMouse("DBLCLK", true));
            if (failure == null && !(element.window == window && element.parentWindow && element.propertyImage != null && element.changeKey != null && element.showChangeKey
                                     && element.changeMouse != null && element.showChangeMouse))
                failure = "the element kept the window " + element.window + " parent " + element.parentWindow + ", the image property " +
                          element.propertyImage + ", the key " + element.changeKey + " shown " + element.showChangeKey +
                          ", the mouse binding " + element.changeMouse + " shown " + element.showChangeMouse;

            findProperty("noFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
