package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;
import static platform.base.BaseUtils.reverse;
import static platform.server.logics.property.derived.DerivedProperty.*;
import static platform.server.logics.property.derived.DerivedProperty.createSetAction;

public class ChangeActionProperty<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    private CalcPropertyInterfaceImplement<I> writeFrom;
    protected final CalcPropertyMapImplement<P, I> writeTo; // assert что здесь + в mapInterfaces полный набор ключей
    protected final CalcPropertyMapImplement<?, I> where;

    public ChangeActionProperty(String sID,
                                String caption,
                                Collection<I> innerInterfaces,
                                List<I> mapInterfaces, CalcPropertyMapImplement<?, I> where, CalcPropertyMapImplement<P, I> writeTo,
                                CalcPropertyInterfaceImplement<I> writeFrom) {
        super(sID, caption, innerInterfaces, mapInterfaces);

        this.writeTo = writeTo;
        this.writeFrom = writeFrom;
        this.where = where;

        assert BaseUtils.mergeColSet(mapInterfaces, writeTo.getInterfaces()).equals(new HashSet<I>(innerInterfaces));

        finalizeInit();
    }

    public Set<ActionProperty> getDependActions() {
        return new HashSet<ActionProperty>();
    }

    @Override
    public Set<CalcProperty> getUsedProps() {
        if(where!=null)
            return getUsedProps(writeFrom, where);
        return getUsedProps(writeFrom);
    }

    @Override
    public QuickSet<CalcProperty> aspectChangeExtProps() {
        return getChangeProps(writeTo.property);
    }

    @Override
    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, Map<I, KeyExpr> innerKeys, Map<I, DataObject> innerValues, Map<I, Expr> innerExprs) throws SQLException {
        // если не хватает ключей надо or добавить, так чтобы кэширование работало
        Collection<I> extInterfaces = BaseUtils.filterNot(innerInterfaces, mapInterfaces.values());
        CalcPropertyMapImplement<?, I> changeWhere = (where == null && extInterfaces.isEmpty()) || (where != null && where.mapIsFull(extInterfaces)) ?
                (where == null ? DerivedProperty.<I>createTrue() : where) : getFullProperty();

        Where exprWhere = changeWhere.mapExpr(innerExprs, context.getModifier()).getWhere();
        if(!exprWhere.isFalse()) // оптимизация, важна так как во многих event'ах может учавствовать
            context.getEnv().change(writeTo.property, new PropertyChange<P>(innerJoin(writeTo.mapping, innerValues), rightJoin(writeTo.mapping, innerKeys), // нет FormEnvironment так как заведомо не action
                    writeFrom.mapExpr(innerExprs, context.getModifier()), exprWhere));

        return FlowResult.FINISH;
    }

    public static <I extends PropertyInterface> CalcPropertyMapImplement<?, I> getFullProperty(Collection<I> innerInterfaces, CalcPropertyMapImplement<?, I> where, CalcPropertyMapImplement<?, I> writeTo, CalcPropertyInterfaceImplement<I> writeFrom) {
        CalcPropertyMapImplement<?, I> result = DerivedProperty.createUnion(innerInterfaces, // проверяем на is WriteClass (можно было бы еще на интерфейсы проверить но пока нет смысла)
                DerivedProperty.createNotNull(writeTo), getValueClassProperty(writeTo, writeFrom));
        if(where!=null)
            result = createAnd(innerInterfaces, where, result);
        return result;
    }

    public static <I extends PropertyInterface> CalcPropertyMapImplement<?, I> getValueClassProperty(CalcPropertyMapImplement<?, I> writeTo, CalcPropertyInterfaceImplement<I> writeFrom) {
        return DerivedProperty.createJoin(IsClassProperty.getProperty(writeTo.property.getValueClass(), "value").
                mapImplement(Collections.singletonMap("value", writeFrom)));
    }

    @IdentityLazy
    private CalcPropertyMapImplement<?, I> getFullProperty() {
        return getFullProperty(innerInterfaces, where, writeTo, writeFrom);
    }

    protected CalcPropertyMapImplement<?, I> getGroupWhereProperty() {
        return getFullProperty();
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(Map<PropertyInterface, T> mapping, Collection<T> context, boolean ordersNotNull) {
        return !ordersNotNull;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(Map<PropertyInterface, T> mapping, Collection<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return null;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(Map<PropertyInterface, T> mapping, Collection<T> context, CalcPropertyMapImplement<PW, T> push, OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForActionProperty.pushFor(innerInterfaces, where, mapInterfaces, mapping, context, push, orders, ordersNotNull, new ForActionProperty.PushFor<I, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(Collection<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, OrderedMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, Map<I, PropertyInterface> mapInnerInterfaces) {
                return createSetAction(context, writeTo.map(mapInnerInterfaces), writeFrom.map(mapInnerInterfaces), where, orders, ordersNotNull);
            }
        });
    }
}
