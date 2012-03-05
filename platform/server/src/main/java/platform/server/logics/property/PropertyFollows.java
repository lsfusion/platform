package platform.server.logics.property;

import platform.server.data.expr.KeyExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.DataSession;
import platform.server.session.IncrementApply;

import java.sql.SQLException;
import java.util.Map;

public class PropertyFollows<T extends PropertyInterface, L extends PropertyInterface> {
    private final Property<T> property;
    private final PropertyMapImplement<L, T> implement;
    private int options;
    public final static int RESOLVE_TRUE = 1;
    public final static int RESOLVE_FALSE = 2;
    public final static int RESOLVE_ALL = RESOLVE_TRUE | RESOLVE_FALSE;
    public final static int RESOLVE_NOTHING = 0;

    public PropertyFollows(Property<T> property, PropertyMapImplement<L, T> implement, int options) {
        this.property = property;
        this.implement = implement;
        this.options = options;
    }

    public Property getFollow() {
        return implement.property;
    }

    public void resolveTrue(DataSession session, boolean recalculate) throws SQLException {
        if((!recalculate && !property.hasChanges(session.modifier)) || ((options & RESOLVE_TRUE) == 0)) // для оптимизации в общем то
            return;

//      f' and !f and !g(')	: g -> NotNull
        Map<T,KeyExpr> mapKeys = property.getMapKeys();
        WhereBuilder followWhere = new WhereBuilder();
        if(recalculate)
            followWhere.add(property.getExpr(mapKeys, session.modifier).getWhere());
        else
            property.getIncrementExpr(mapKeys, session.modifier, followWhere, IncrementType.SET);
        implement.mapJoinNotNull(mapKeys, followWhere.toWhere(), session, session.modifier);
    }

    public void resolveFalse(DataSession session, boolean recalculate) throws SQLException {
        if((!recalculate && !implement.property.hasChanges(session.modifier)) || ((options & RESOLVE_FALSE) == 0)) // для оптимизации в общем то
            return;

//      f(') and g and !g' : f -> Null
        Map<T,KeyExpr> mapKeys = property.getMapKeys();
        WhereBuilder followWhere = new WhereBuilder();
        if(recalculate)
            followWhere.add(implement.mapExpr(mapKeys, session.modifier).getWhere().not());
        else
            implement.mapIncrementExpr(mapKeys, session.modifier, followWhere, IncrementType.DROP);
        property.setNull(mapKeys, followWhere.toWhere(), session, session.modifier);
    }

    public void resolveTrue(IncrementApply incrementApply) throws SQLException {
        if(!(property.hasChanges(incrementApply) || property.hasChanges(incrementApply.applyStart) ) || ((options & RESOLVE_TRUE) == 0)) // для оптимизации в общем то
            return;

//      f' and !f and !g(')	: g -> NotNull
        Map<T,KeyExpr> mapKeys = property.getMapKeys();
        WhereBuilder followWhere = new WhereBuilder();
        property.getIncrementExpr(mapKeys, incrementApply, incrementApply.applyStart, followWhere, IncrementType.SET);
        implement.mapJoinNotNull(mapKeys, followWhere.toWhere(), incrementApply.session, incrementApply);
    }

    public void resolveFalse(IncrementApply incrementApply) throws SQLException {
        if(!(implement.property.hasChanges(incrementApply) || implement.property.hasChanges(incrementApply.applyStart)) || ((options & RESOLVE_FALSE) == 0)) // для оптимизации в общем то
            return;

//      f(') and g and !g' : f -> Null
        Map<T,KeyExpr> mapKeys = property.getMapKeys();
        WhereBuilder followWhere = new WhereBuilder();
        implement.mapIncrementExpr(mapKeys, incrementApply, incrementApply.applyStart, followWhere, IncrementType.DROP);
        property.setNull(mapKeys, followWhere.toWhere(), incrementApply.session, incrementApply);
    }
}
