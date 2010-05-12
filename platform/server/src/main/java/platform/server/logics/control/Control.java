package platform.server.logics.control;

import platform.server.logics.property.group.AbstractNode;
import platform.server.logics.control.ControlInterface;
import platform.server.logics.linear.LC;
import platform.server.data.where.classes.ClassWhere;
import platform.server.caches.GenericImmutable;
import platform.server.caches.GenericLazy;
import platform.server.classes.sets.AndClassSet;

import java.util.Collection;
import java.util.Map;
import java.util.List;

@GenericImmutable
public abstract class Control<T extends ControlInterface> extends AbstractNode {

    public final String sID;

    public String caption;

    public String toString() {
        return caption;
    }

    public int ID=0;

    public final Collection<T> interfaces;

    public Control(String sID, String caption, Collection<T> interfaces) {
        this.sID = sID;
        this.caption = caption;
        this.interfaces = interfaces;
    }

    public abstract ClassWhere<T> getClassWhere();

    public boolean check() {
        return !getClassWhere().isFalse();
    }

    public <P extends ControlInterface> boolean intersect(Control<P> control, Map<P,T> map) {
        return !getClassWhere().and(new ClassWhere<T>(control.getClassWhere(),map)).isFalse();
    }

    @GenericLazy
    public boolean allInInterface(Map<T,? extends AndClassSet> interfaceClasses) {
        return new ClassWhere<T>(interfaceClasses).meansCompatible(getClassWhere());
    }

    @GenericLazy
    public boolean anyInInterface(Map<T, ? extends AndClassSet> interfaceClasses) {
        return !getClassWhere().andCompatible(new ClassWhere<T>(interfaceClasses)).isFalse();
    }


    public abstract LC createLC(List<T> listInterfaces);
}
