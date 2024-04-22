package lsfusion.server.logics.property.implement;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.*;
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
import lsfusion.server.logics.action.session.change.CalcDataType;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapChange;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.classes.infer.ClassType;
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

    void mapFillDepends(MSet<Property> depends);
    ImSet<OldProperty> mapOldDepends();
    
    int mapHashCode();
    boolean mapEquals(PropertyInterfaceImplement<P> implement);

    Object read(ExecutionContext context, ImMap<P, ? extends ObjectValue> interfaceValues) throws SQLException, SQLHandledException;
    default ObjectValue readClasses(ExecutionContext<P> context) throws SQLException, SQLHandledException {
        return readClasses(context, context.getKeys());
    }
    ObjectValue readClasses(ExecutionContext context, ImMap<P, ? extends ObjectValue> interfaceValues) throws SQLException, SQLHandledException;

    ActionMapImplement<?, P> mapEventAction(String eventSID, FormSessionScope defaultChangeEventScope, ImList<Property> viewProperties, String customChangeFunction);

    Property.Select<P> mapSelect(ImList<Property> viewProperties, boolean forceSelect);
    boolean mapNameValueUnique();

    boolean mapIsDrawNotNull();
    boolean mapIsNotNull();
    boolean mapIsExplicitTrue();
    boolean mapHasAlotKeys();
    int mapEstComplexity();

    Pair<PropertyInterfaceImplement<P>, PropertyInterfaceImplement<P>> getIfProp();

    ImSet<DataProperty> mapChangeProps();
    boolean mapHasPreread(PropertyChanges propertyChanges);
    boolean mapHasPreread(Modifier modifier) throws SQLException, SQLHandledException;

    long mapSimpleComplexity();
    DataChanges mapJoinDataChanges(ImMap<P, ? extends Expr> mapKeys, Expr expr, Where where, GroupType groupType, WhereBuilder changedWhere, PropertyChanges propChanges, CalcDataType type);
    DataChanges mapJoinDataChanges(PropertyChange<P> change, CalcDataType type, GroupType groupType, WhereBuilder changedWhere, PropertyChanges propChanges);

    void fill(MSet<P> interfaces, MSet<PropertyMapImplement<?, P>> properties);
    ImSet<P> getInterfaces();

    Inferred<P> mapInferInterfaceClasses(ExClassSet commonValue, InferType inferType);
    boolean mapNeedInferredForValueClass(InferType inferType);
    ExClassSet mapInferValueClass(ImMap<P, ExClassSet> inferred, InferType inferType);
    ValueClass mapValueClass(ClassType classType);

    boolean mapIsFull(ImSet<P> interfaces);
    boolean mapHasNoGridReadOnly(ImSet<P> gridInterfaces);

    AndClassSet mapValueClassSet(ClassWhere<P> interfaceClasses);

    <X extends PropertyInterface> AsyncMapChange<X, P> mapAsyncChange(PropertyMapImplement<X, P> writeTo, ObjectEntity object);

    Graph<CalcCase<P>> mapAbstractGraph();

    boolean mapChangedWhen(boolean toNull, PropertyInterfaceImplement<P> changeProperty);
    boolean mapIsExplicitNot(PropertyInterfaceImplement<P> where);
//    OrderEntity mapEntityObjects(ImRevMap<P, ObjectEntity> mapObjects);
//
//    <C extends PropertyInterface> PropertyInterfaceImplement<C> mapInner(ImRevMap<P, C> map);
//
//    <C extends PropertyInterface> PropertyInterfaceImplement<C> mapJoin(ImMap<P, PropertyInterfaceImplement<C>> map);
}
