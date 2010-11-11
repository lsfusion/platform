package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.MapDataChanges;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface PropertyInterfaceImplement<P extends PropertyInterface> {

    Expr mapExpr(Map<P, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere);

    abstract void mapFillDepends(Collection<Property> depends);

    ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, Modifier<? extends Changes> modifier) throws SQLException;

    MapDataChanges<P> mapJoinDataChanges(Map<P, KeyExpr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, Modifier<? extends Changes> modifier);

    void fill(Set<P> interfaces, Set<PropertyMapImplement<?,P>> properties);
    public <K extends PropertyInterface> PropertyInterfaceImplement<K> map(Map<P,K> remap);
}
