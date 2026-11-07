package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.NavigatorFolder;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// two modules that do not require each other setting up one navigator element, the later one in the module order
// writing first, as the parse threads' timing can have it : the element keeps the later module's caption, window with
// its PARENT and image with its property. The element is one of the test's own, out of the navigator. What the test saw
// goes into noOrderFailure
public class SetNavigatorOptionsInModuleOrderAction extends AtOnceAction {

    public SetNavigatorOptionsInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            LogicsModule[] modules = independentModules(context.getBL(), LM);
            NavigatorWindow earlierWindow = (NavigatorWindow) context.getBL().findWindow("System.root");
            NavigatorWindow laterWindow = (NavigatorWindow) context.getBL().findWindow("System.toolbar");
            Property<?> earlierImage = findProperty("noEarlierImage[]").property;
            Property<?> laterImage = findProperty("noLaterImage[]").property;

            NavigatorElement element = new NavigatorFolder("NavigatorOptions.noFolder");
            element.setCaption(() -> LocalizedString.create("later", false), modules[1].getVersion());
            element.setWindow(laterWindow, true, modules[1].getVersion());
            element.setImage(AppServerImage.AUTO, laterImage, modules[1].getVersion());
            element.setCaption(() -> LocalizedString.create("earlier", false), modules[0].getVersion());
            element.setWindow(earlierWindow, false, modules[0].getVersion());
            element.setImage(AppServerImage.AUTO, earlierImage, modules[0].getVersion());

            String failure = null;
            if (!("later".equals(element.getCaption()) && element.getWindow() == laterWindow && element.isParentWindow() && element.getPropertyImage() == laterImage))
                failure = "the element kept the caption " + element.getCaption() + ", the window " + element.getWindow() + " parent " + element.isParentWindow()
                        + ", the image property " + element.getPropertyImage() + " - not all of " + modules[1].getName() + ", later in the module order than " + modules[0].getName();

            findProperty("noOrderFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
