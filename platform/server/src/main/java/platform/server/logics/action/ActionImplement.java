package platform.server.logics.action;

import platform.server.logics.control.ControlImplement;
import platform.server.logics.control.ControlInterface;

import java.util.Map;

public class ActionImplement<T, P extends ActionInterface> extends ControlImplement<T, P, Action<P>> {
    
    public ActionImplement(Action<P> property, Map<P, T> mapping) {
        super(property, mapping);
    }

    public ActionImplement(Action<P> property) {
        super(property);
    }
}
