package platform.server.logics.linear;

import platform.server.logics.action.ActionInterface;
import platform.server.logics.action.Action;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.navigator.ActionObjectNavigator;

import java.util.List;

public class LA extends LC<ActionInterface, Action> {

    public LA(Action property) {
        super(property);
    }

    public LA(Action property, List<ActionInterface> listInterfaces) {
        super(property, listInterfaces);
    }

    public ActionObjectNavigator createNavigator(ObjectNavigator... objects) {
        return new ActionObjectNavigator(this, objects);
    }
}
