package platform.server.logics.action;

import platform.server.logics.control.ControlImplement;
import platform.server.logics.control.ControlInterface;

import java.util.Map;

public class ActionImplement<T> extends ControlImplement<T, ActionInterface, Action> {
    
    public ActionImplement(Action property, Map<ActionInterface, T> mapping) {
        super(property, mapping);
    }

    public ActionImplement(Action property) {
        super(property);
    }
}
