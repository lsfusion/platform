package lsfusion.server.logics.property.implement;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.oraction.ActionOrPropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public interface PropertyInterfaceImplement<P extends PropertyInterface> extends ActionOrPropertyInterfaceImplement {

    <T extends PropertyInterface> PropertyInterfaceImplement<T> map(ImRevMap<P, T> map);

    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges changes, WhereBuilder changedWhere);
    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere);
    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, PropertyChanges propChanges);
    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, Modifier modifier) throws SQLException, SQLHandledException;
    Expr mapExpr(ImMap<P, ? extends Expr> joinImplement);

    void mapFillDepends(MSet<Property> depends);
    ImSet<OldProperty> mapOldDepends();
    
    int mapHashCode();
    boolean mapEquals(PropertyInterfaceImplement<P> implement);

    Object read(ExecutionContext context, ImMap<P, ? extends ObjectValue> interfaceValues) throws SQLException, SQLHandledException;
    default ObjectValue readClasses(ExecutionContext<P> context) throws SQLException, SQLHandledException {
        return readClasses(context, context.getKeys());
    }
    ObjectValue readClasses(ExecutionContext context, ImMap<P, ? extends ObjectValue> interfaceValues) throws SQLException, SQLHandledException;

    ActionMapImplement<?, P> mapEventAction(String eventSID, Property filterProperty);

    boolean mapHasAlotKeys();
    int mapEstComplexity();
    
    ImSet<DataProperty> mapChangeProps();
    boolean mapIsOrDependsPreread();

    long mapComplexity();
    DataChanges mapJoinDataChanges(ImMap<P, ? extends Expr> mapKeys, Expr expr, Where where, GroupType type, WhereBuilder changedWhere, PropertyChanges propChanges);
    DataChanges mapJoinDataChanges(PropertyChange<P> change, GroupType type, WhereBuilder changedWhere, PropertyChanges propChanges);

    void fill(MSet<P> interfaces, MSet<PropertyMapImplement<?, P>> properties);
    ImCol<P> getInterfaces();

    Inferred<P> mapInferInterfaceClasses(ExClassSet commonValue, InferType inferType);
    boolean mapNeedInferredForValueClass(InferType inferType);
    ExClassSet mapInferValueClass(ImMap<P, ExClassSet> inferred, InferType inferType);

    boolean mapIsFull(ImSet<P> interfaces);

    AndClassSet mapValueClassSet(ClassWhere<P> interfaceClasses);

    Graph<CalcCase<P>> mapAbstractGraph();
}
