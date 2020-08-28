package lsfusion.server.logics.action.change;

import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.ExtendContextAction;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.ForAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.value.ValueProperty;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

import static lsfusion.server.logics.property.PropertyFact.createSetAction;

public class SetAction<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends ExtendContextAction<I> {

    private PropertyInterfaceImplement<I> writeFrom;
    protected final PropertyMapImplement<P, I> writeTo; // assert что здесь + в mapInterfaces полный набор ключей
    protected final PropertyMapImplement<?, I> where;
    
    public static boolean hasFlow(PropertyMapImplement<?,?> writeTo, ChangeFlowType type) {
        if(type.isChange() && writeTo.property.canBeGlobalChanged())
            return true;             
        return false;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(hasFlow(writeTo, type))
            return true;
        return super.hasFlow(type);
    }

    public SetAction(LocalizedString caption,
                     ImSet<I> innerInterfaces,
                     ImOrderSet<I> mapInterfaces, PropertyMapImplement<?, I> where, PropertyMapImplement<P, I> writeTo,
                     PropertyInterfaceImplement<I> writeFrom) {
        super(caption, innerInterfaces, mapInterfaces);

        this.writeTo = writeTo;
        this.writeFrom = writeFrom;
        this.where = where;

        assert mapInterfaces.getSet().merge(writeTo.getInterfaces()).equals(innerInterfaces);

        finalizeInit();
    }

    public ImSet<Action> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        if(where!=null)
            return getUsedProps(writeFrom, where);
        return getUsedProps(writeFrom);
    }

    @Override
    public ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeProps(writeTo.property);
    }

    @Override
    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        if((where == null || where.property instanceof ValueProperty) && writeTo.property instanceof SessionDataProperty && !writeTo.mapping.valuesSet().intersect(mapInterfaces.valuesSet())
                && !(writeFrom instanceof PropertyMapImplement && Property.depends(((PropertyMapImplement)writeFrom).property, writeTo.property))) // оптимизация, в дальнейшем надо будет непосредственно в aspectChangeProperty сделать в случае SessionDataProperty ставить "удалить" изменения на null
            session.dropChanges((SessionDataProperty) writeTo.property);

        // если не хватает ключей надо or добавить, так чтобы кэширование работало
        ImSet<I> extInterfaces = innerInterfaces.remove(mapInterfaces.valuesSet());
        PropertyMapImplement<?, I> changeWhere = (where == null && extInterfaces.isEmpty()) || (where != null && where.mapIsFull(extInterfaces) && !(writeTo.property instanceof SessionDataProperty)) ?
                (where == null ? getTrueProperty() : where) : getFullProperty();

        Where exprWhere = changeWhere.mapExpr(innerExprs, context.getModifier()).getWhere();

        if(!exprWhere.isFalse()) { // оптимизация, важна так как во многих event'ах может учавствовать

            Result<SessionTableUsage> rUsedTable = new Result<>();
            try {
                if (writeFrom.mapHasPreread(context.getModifier()) && PropertyChange.needMaterializeWhere(exprWhere)) // оптимизация с materialize'ингом
                    exprWhere = PropertyChange.materializeWhere("setmwh", changeWhere, session, innerKeys, innerValues, innerExprs, exprWhere, rUsedTable);

                if (!exprWhere.isFalse()) {
                    Expr fromExpr = writeFrom.mapExpr(PropertyChange.simplifyExprs(innerExprs, exprWhere), context.getModifier());
                    ImMap<P, DataObject> writeInnerValues = DataObject.onlyDataObjects(writeTo.mapping.innerJoin(innerValues));
                    if (writeInnerValues != null) {
                        context.getEnv().change(writeTo.property, new PropertyChange<>(writeInnerValues, writeTo.mapping.rightJoin(innerKeys), // нет FormEnvironment так как заведомо не action
                                fromExpr, exprWhere));
                        SQLSession.checkSessionTableAssertion(context.getModifier());
                    } else
                        proceedNullException();
                }
            } finally {
                if(rUsedTable.result!=null)
                    rUsedTable.result.drop(session.sql, session.getOwner());
            }
        }

        return FlowResult.FINISH;
    }

    public static <I extends PropertyInterface> PropertyMapImplement<?, I> getFullProperty(ImSet<I> innerInterfaces, PropertyMapImplement<?, I> where, PropertyMapImplement<?, I> writeTo, PropertyInterfaceImplement<I> writeFrom) {
        PropertyMapImplement<?, I> result = PropertyFact.createUnion(innerInterfaces, // проверяем на is WriteClass (можно было бы еще на интерфейсы проверить но пока нет смысла)
                PropertyFact.createNotNull(writeTo), getValueClassProperty(writeTo, writeFrom));
        if(where!=null)
            result = PropertyFact.createAnd(innerInterfaces, where, result);
        return result;
    }

    public static <I extends PropertyInterface> PropertyMapImplement<?, I> getValueClassProperty(PropertyMapImplement<?, I> writeTo, PropertyInterfaceImplement<I> writeFrom) {
        return PropertyFact.createJoin(IsClassProperty.getProperty(writeTo.property.getValueClass(ClassType.wherePolicy), "value").
                mapImplement(MapFact.singleton("value", writeFrom)));
    }

    @IdentityInstanceLazy
    private PropertyMapImplement<?, I> getFullProperty() {
        return getFullProperty(innerInterfaces, where, writeTo, writeFrom);
    }

    @IdentityInstanceLazy
    private <T extends PropertyInterface> PropertyMapImplement<?, T> getTrueProperty() { // to avoid property leaks
        return PropertyFact.createTrue();
    }

    protected PropertyMapImplement<?, I> calcGroupWhereProperty() {
        return getFullProperty();
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return !ordersNotNull;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> Property getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        return null;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, PropertyMapImplement<PW, T> push, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForAction.pushFor(innerInterfaces, where, mapInterfaces, mapping, context, push, orders, ordersNotNull, (context1, where, orders1, ordersNotNull1, mapInnerInterfaces) -> createSetAction(context1, writeTo.map(mapInnerInterfaces), writeFrom.map(mapInnerInterfaces), where, orders1, ordersNotNull1));
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // need this for property breakpoints
    }
}
