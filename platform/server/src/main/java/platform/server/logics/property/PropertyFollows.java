package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Map;

public class PropertyFollows<T extends PropertyInterface, L extends PropertyInterface> {
    private final Property<T> property;
    private final PropertyMapImplement<L, T> implement;

    public PropertyFollows(Property<T> property, PropertyMapImplement<L, T> implement) {
        this.property = property;
        this.implement = implement;
    }

    public Property getFollow() {
        return implement.property;
    }

    public void resolveTrue(DataSession session, BusinessLogics<?> BL) throws SQLException {
        if(!property.hasChanges(session.modifier)) // для оптимизации в общем то
            return;

//      f' and !f and !g(')	: g -> NotNull
//      в общем то чтобы исключить избыточность надо ровно в одном из resolve'ов оставить новое значение, а в другом старое
        Map<T,KeyExpr> mapKeys = property.getMapKeys();
        implement.mapNotNull(mapKeys, property.getExpr(mapKeys, session.modifier).getWhere().
                and(property.getExpr(mapKeys).getWhere().not()).
                and(implement.mapExpr(mapKeys, session.modifier).getWhere().not()), session, BL);
    }

    public void resolveFalse(DataSession session, BusinessLogics<?> BL) throws SQLException {
        if(!implement.property.hasChanges(session.modifier)) // для оптимизации в общем то
            return;

//      f(') and g and !g' : f -> Null
        Map<T,KeyExpr> mapKeys = property.getMapKeys();
        property.setNull(mapKeys, property.getExpr(mapKeys).getWhere().
                        and(implement.mapExpr(mapKeys).getWhere()).
                        and(implement.mapExpr(mapKeys, session.modifier).getWhere().not()), session, BL);
    }

}
