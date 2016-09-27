package lsfusion.server.logics.property.actions.flow;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.debug.ActionDelegationType;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.NoPropertyTableUsage;
import lsfusion.server.session.PropertyChange;

import java.sql.SQLException;

import static lsfusion.server.logics.property.derived.DerivedProperty.createSetAction;

public class SetActionProperty<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    private CalcPropertyInterfaceImplement<I> writeFrom;
    protected final CalcPropertyMapImplement<P, I> writeTo; // assert что здесь + в mapInterfaces полный набор ключей
    protected final CalcPropertyMapImplement<?, I> where;

    public SetActionProperty(LocalizedString caption,
                             ImSet<I> innerInterfaces,
                             ImOrderSet<I> mapInterfaces, CalcPropertyMapImplement<?, I> where, CalcPropertyMapImplement<P, I> writeTo,
                             CalcPropertyInterfaceImplement<I> writeFrom) {
        super(caption, innerInterfaces, mapInterfaces);

        this.writeTo = writeTo;
        this.writeFrom = writeFrom;
        this.where = where;

        assert mapInterfaces.getSet().merge(writeTo.getInterfaces()).equals(innerInterfaces);

        finalizeInit();
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        if(where!=null)
            return getUsedProps(writeFrom, where);
        return getUsedProps(writeFrom);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(writeTo.property);
    }

    @Override
    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        if((where == null || where.property instanceof ValueProperty) && writeTo.property instanceof SessionDataProperty && !writeTo.mapping.valuesSet().intersect(mapInterfaces.valuesSet())
                && !(writeFrom instanceof CalcPropertyMapImplement && CalcProperty.depends(((CalcPropertyMapImplement)writeFrom).property, writeTo.property))) // оптимизация, в дальнейшем надо будет непосредственно в aspectChangeProperty сделать в случае SessionDataProperty ставить "удалить" изменения на null
            session.dropChanges((SessionDataProperty) writeTo.property);

        // если не хватает ключей надо or добавить, так чтобы кэширование работало
        ImSet<I> extInterfaces = innerInterfaces.remove(mapInterfaces.valuesSet());
        CalcPropertyMapImplement<?, I> changeWhere = (where == null && extInterfaces.isEmpty()) || (where != null && where.mapIsFull(extInterfaces)) ?
                (where == null ? DerivedProperty.<I>createTrue() : where) : getFullProperty();

        Where exprWhere = changeWhere.mapExpr(innerExprs, context.getModifier()).getWhere();

        if(!exprWhere.isFalse()) { // оптимизация, важна так как во многих event'ах может учавствовать

            NoPropertyTableUsage<I> mapTable = null;
            if(writeFrom.mapIsComplex() && PropertyChange.needMaterializeWhere(exprWhere)) { // оптимизация с materialize'ингом
                mapTable = PropertyChange.<I>materializeWhere(session, innerKeys, exprWhere);
                exprWhere = mapTable.join(innerKeys).getWhere();
            }

            if(!exprWhere.isFalse()) {
                Expr fromExpr = writeFrom.mapExpr(PropertyChange.simplifyExprs(innerExprs, exprWhere), context.getModifier());
                ImMap<P, DataObject> writeInnerValues = DataObject.onlyDataObjects(writeTo.mapping.innerJoin(innerValues));
                if(writeInnerValues!=null)
                    context.getEnv().change(writeTo.property, new PropertyChange<>(writeInnerValues, writeTo.mapping.rightJoin(innerKeys), // нет FormEnvironment так как заведомо не action
                            fromExpr, exprWhere));
                else
                    proceedNullException();
            }

            if(mapTable!=null)
                mapTable.drop(session.sql, session.getOwner());
        }

        return FlowResult.FINISH;
    }

    public static <I extends PropertyInterface> CalcPropertyMapImplement<?, I> getFullProperty(ImSet<I> innerInterfaces, CalcPropertyMapImplement<?, I> where, CalcPropertyMapImplement<?, I> writeTo, CalcPropertyInterfaceImplement<I> writeFrom) {
        CalcPropertyMapImplement<?, I> result = DerivedProperty.createUnion(innerInterfaces, // проверяем на is WriteClass (можно было бы еще на интерфейсы проверить но пока нет смысла)
                DerivedProperty.createNotNull(writeTo), getValueClassProperty(writeTo, writeFrom));
        if(where!=null)
            result = DerivedProperty.createAnd(innerInterfaces, where, result);
        return result;
    }

    public static <I extends PropertyInterface> CalcPropertyMapImplement<?, I> getValueClassProperty(CalcPropertyMapImplement<?, I> writeTo, CalcPropertyInterfaceImplement<I> writeFrom) {
        return DerivedProperty.createJoin(IsClassProperty.getProperty(writeTo.property.getValueClass(ClassType.wherePolicy), "value").
                mapImplement(MapFact.singleton("value", writeFrom)));
    }

    @IdentityInstanceLazy
    private CalcPropertyMapImplement<?, I> getFullProperty() {
        return getFullProperty(innerInterfaces, where, writeTo, writeFrom);
    }

    protected CalcPropertyMapImplement<?, I> calcGroupWhereProperty() {
        return getFullProperty();
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return !ordersNotNull;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return null;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> push, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForActionProperty.pushFor(innerInterfaces, where, mapInterfaces, mapping, context, push, orders, ordersNotNull, new ForActionProperty.PushFor<I, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(ImSet<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, ImRevMap<I, PropertyInterface> mapInnerInterfaces) {
                return createSetAction(context, writeTo.map(mapInnerInterfaces), writeFrom.map(mapInnerInterfaces), where, orders, ordersNotNull);
            }
        });
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // поменяли во время добавления дебага изменения data свойств, чтобы в стеке дебаггера отображались
    }
}
