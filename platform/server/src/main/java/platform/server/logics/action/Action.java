package platform.server.logics.action;

import platform.server.logics.control.Control;
import platform.server.logics.action.ActionInterface;
import platform.server.logics.linear.LC;
import platform.server.logics.linear.LA;
import platform.server.logics.DataObject;
import platform.server.data.where.classes.ClassWhere;
import platform.interop.form.RemoteFormInterface;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class Action<P extends ActionInterface> extends Control<P> {

    public Action(String sID, String caption, Collection<P> interfaces) {
        super(sID, caption, interfaces);
    }

    public abstract RemoteFormInterface execute(Map<P, DataObject> objects);

    public LC createLC(List<P> listInterfaces) {
        return new LA<P>(this, listInterfaces);
    }
}
