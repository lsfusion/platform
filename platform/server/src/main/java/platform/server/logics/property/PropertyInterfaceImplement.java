package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.sql.SQLException;

public interface PropertyInterfaceImplement<P extends PropertyInterface> {

    Expr mapExpr(Map<P, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere);

    abstract void mapFillDepends(Collection<Property> depends);

    ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, Modifier<? extends Changes> modifier) throws SQLException;

    DataChanges mapJoinDataChanges(Map<P,? extends Expr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, Modifier<? extends Changes> modifier);

    void fill(Set<P> interfaces, Set<PropertyMapImplement<?,P>> properties);
    public <K extends PropertyInterface> PropertyInterfaceImplement<K> map(Map<P,K> remap);
}
