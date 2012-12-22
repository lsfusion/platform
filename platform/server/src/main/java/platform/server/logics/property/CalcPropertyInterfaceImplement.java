package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.sql.SQLException;
import java.util.Set;

public interface CalcPropertyInterfaceImplement<P extends PropertyInterface> extends PropertyInterfaceImplement<P> {

    <T extends PropertyInterface> CalcPropertyInterfaceImplement<T> map(ImRevMap<P, T> map);

    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere);
    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere);
    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, PropertyChanges propChanges);
    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, Modifier modifier);
    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement);

    void mapFillDepends(MSet<CalcProperty> depends);
    ImSet<OldProperty> mapOldDepends();

    Object read(ExecutionContext context, ImMap<P, DataObject> interfaceValues) throws SQLException;
    ObjectValue readClasses(ExecutionContext context, ImMap<P, DataObject> interfaceValues) throws SQLException;

    public ActionPropertyMapImplement<?, P> mapEditAction(String editActionSID, CalcProperty filterProperty);

    ImSet<DataProperty> mapChangeProps();
    DataChanges mapJoinDataChanges(ImMap<P, ? extends Expr> mapKeys, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges);

    DataChanges mapDataChanges(PropertyChange<P> change, WhereBuilder changedWhere, PropertyChanges propChanges);

    void fill(MSet<P> interfaces, MSet<CalcPropertyMapImplement<?, P>> properties);
    ImCol<P> getInterfaces();

    ImMap<P, ValueClass> mapInterfaceCommonClasses(ValueClass commonValue);
}
