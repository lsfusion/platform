package lsfusion.server.logics.navigator.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import org.apache.log4j.Logger;

// the windows' CUSTOM, settled once every module has run : two modules that do not require each other can give one window
// a React component and a property, which no statement of either sees (AbstractWindow.setCustoms)
public class CheckWindowsTask extends SimpleBLTask {

    public String getCaption() {
        return "Checking windows";
    }

    public void run(Logger logger) {
        for (AbstractWindow window : getBL().getWindows())
            window.checkCustoms();
    }
}
