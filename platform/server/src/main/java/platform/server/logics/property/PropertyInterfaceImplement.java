package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;
import platform.server.session.MapDataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChanges;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface PropertyInterfaceImplement<P extends PropertyInterface> {

    Expr mapExpr(Map<P, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere);
    Expr mapExpr(Map<P, ? extends Expr> joinImplement, PropertyChanges propChanges);
    Expr mapExpr(Map<P, ? extends Expr> joinImplement);

    Expr mapIncrementExpr(Map<P, ? extends Expr> joinImplement, PropertyChanges newChanges, PropertyChanges prevChanges, WhereBuilder changedWhere, IncrementType incrementType);

    abstract void mapFillDepends(Collection<Property> depends);

    Object read(DataSession session, Map<P, DataObject> interfaceValues, Modifier modifier) throws SQLException;
    ObjectValue readClasses(DataSession session, Map<P, DataObject> interfaceValues, Modifier modifier) throws SQLException;

    public PropertyMapImplement<?, P> mapChangeImplement(Map<P, DataObject> interfaceValues, DataSession session, Modifier modifier) throws SQLException;

    MapDataChanges<P> mapJoinDataChanges(Map<P, KeyExpr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges);

    void fill(Set<P> interfaces, Set<PropertyMapImplement<?,P>> properties);
    public <K extends PropertyInterface> PropertyInterfaceImplement<K> map(Map<P,K> remap);
}
