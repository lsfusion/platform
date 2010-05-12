package platform.server.logics.linear;

import platform.server.logics.action.ActionInterface;
import platform.server.logics.action.Action;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.navigator.ActionObjectNavigator;

import java.util.List;

public class LA<P extends ActionInterface> extends LC<P, Action<P>> {

    public LA(Action<P> property) {
        super(property);
    }

    public LA(Action<P> property, List<P> listInterfaces) {
        super(property, listInterfaces);
    }

    public ActionObjectNavigator<P> createNavigator(ObjectNavigator... objects) {
        return new ActionObjectNavigator<P>(this, objects);
    }
}
