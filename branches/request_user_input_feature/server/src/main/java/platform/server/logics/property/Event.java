package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public abstract class Event<C extends PropertyInterface, P extends Property<C>> {

    protected final P writeTo; // что меняем
    protected final CalcPropertyMapImplement<?, C> where;

    public Event(P writeTo, CalcPropertyMapImplement<?, C> where) {
        assert ((CalcProperty)where.property).noDB();
        this.writeTo = writeTo;
        this.where = where;
    }

    public Set<CalcProperty> getDepends() {
        Set<CalcProperty> used = new HashSet<CalcProperty>();
        where.mapFillDepends(used);
        return used;
    }

    public Set<OldProperty> getOldDepends() {
        Set<OldProperty> result = new HashSet<OldProperty>();
        result.addAll(where.mapOldDepends());
        return result;
    }

    @IdentityLazy
    private boolean isWhereFull() {
        return where.mapIsFull(writeTo.interfaces);
    }
}
