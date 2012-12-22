package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.caches.IdentityLazy;

import java.util.HashSet;
import java.util.Set;

public abstract class Event<C extends PropertyInterface, P extends Property<C>> {

    protected final P writeTo; // что меняем
    protected final CalcPropertyMapImplement<?, C> where;

    public Event(P writeTo, CalcPropertyMapImplement<?, C> where) {
        assert ((CalcProperty)where.property).noDB();
        this.writeTo = writeTo;
        this.where = where;
    }

    public ImSet<CalcProperty> getDepends() {
        MSet<CalcProperty> mUsed = SetFact.mSet();
        where.mapFillDepends(mUsed);
        return mUsed.immutable();
    }

    public ImSet<OldProperty> getOldDepends() {
        return where.mapOldDepends();
    }
}
