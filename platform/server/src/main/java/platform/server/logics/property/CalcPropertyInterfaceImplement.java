package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static platform.base.BaseUtils.crossJoin;

public interface CalcPropertyInterfaceImplement<P extends PropertyInterface> extends PropertyInterfaceImplement<P> {

    <T extends PropertyInterface> CalcPropertyInterfaceImplement<T> map(Map<P, T> map);

    Expr mapExpr(Map<P, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere);
    Expr mapExpr(Map<P, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere);
    Expr mapExpr(Map<P, ? extends Expr> joinImplement, PropertyChanges propChanges);
    Expr mapExpr(Map<P, ? extends Expr> joinImplement, Modifier modifier);
    Expr mapExpr(Map<P, ? extends Expr> joinImplement);

    void mapFillDepends(Set<CalcProperty> depends);
    Set<OldProperty> mapOldDepends();

    Object read(ExecutionContext context, Map<P, DataObject> interfaceValues) throws SQLException;
    ObjectValue readClasses(ExecutionContext context, Map<P, DataObject> interfaceValues) throws SQLException;

    public ActionPropertyMapImplement<?, P> mapEditAction(String editActionSID, CalcProperty filterProperty);

    Collection<DataProperty> mapChangeProps();
    DataChanges mapJoinDataChanges(Map<P, ? extends Expr> mapKeys, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges);

    DataChanges mapDataChanges(PropertyChange<P> change, WhereBuilder changedWhere, PropertyChanges propChanges);

    void fill(Set<P> interfaces, Set<CalcPropertyMapImplement<?,P>> properties);
    Collection<P> getInterfaces();

    Map<P, ValueClass> mapInterfaceCommonClasses(ValueClass commonValue);
}
