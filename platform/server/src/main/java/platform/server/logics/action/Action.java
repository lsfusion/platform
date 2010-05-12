package platform.server.logics.action;

import platform.server.logics.control.Control;
import platform.server.logics.action.ActionInterface;
import platform.server.logics.linear.LC;
import platform.server.logics.linear.LA;
import platform.server.data.where.classes.ClassWhere;

import java.util.Collection;
import java.util.List;

public class Action extends Control<ActionInterface> {

    public Action(String sID, String caption, Collection<ActionInterface> interfaces) {
        super(sID, caption, interfaces);
    }

    public ClassWhere<ActionInterface> getClassWhere() {
        return null;
    }

    public LC createLC(List<ActionInterface> listInterfaces) {
        return new LA(this, listInterfaces);
    }
}
