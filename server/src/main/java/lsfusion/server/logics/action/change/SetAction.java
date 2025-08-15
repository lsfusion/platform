package lsfusion.server.logics.action.change;

import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
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
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapChange;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
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

public class SetAction<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends ExtendContextAction<I> {

    private PropertyInterfaceImplement<I> writeFrom;
    protected final PropertyMapImplement<P, I> writeTo; // assert что здесь + в mapInterfaces полный набор ключей
    protected final PropertyMapImplement<?, I> where;
    
    public static boolean hasFlow(PropertyMapImplement<?,?> writeTo, ChangeFlowType type) {
        if(type.isChange() && writeTo.property.canBeGlobalChanged())
            return true;
        if(type == ChangeFlowType.ANYEFFECT)
            return true;
        return false;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(hasFlow(writeTo, type))
            return true;
        return super.hasFlow(type, recursiveAbstracts);
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
    public ImMap<Property, Boolean> calculateUsedExtProps(ImSet<Action<?>> recursiveAbstracts) {
        if(where!=null)
            return getUsedProps(writeFrom, where);
        return getUsedProps(writeFrom);
    }

    @Override
    public ImMap<Property, Boolean> aspectChangeExtProps(ImSet<Action<?>> recursiveAbstracts) {
        return getChangeProps(writeTo.property);
    }

    @IdentityLazy
    private boolean isRewriteLocal() {
        return writeTo.property instanceof SessionDataProperty && !writeTo.mapping.valuesSet().intersect(this.mapInterfaces.valuesSet()) && (where == null || where.mapIsOr(writeTo))
                && !(writeFrom instanceof PropertyMapImplement && Property.depends(((PropertyMapImplement)writeFrom).property, writeTo.property));
    }

    @Override
    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        if(isRewriteLocal()) // optimization, in the future it will be necessary to directly set “delete” changes to null in aspectChangeProperty in the case of SessionDataProperty
            session.dropChanges((SessionDataProperty) writeTo.property);

        Where exprWhere = where != null ? where.mapExpr(innerExprs, context.getModifier()).getWhere() : Where.TRUE();

        if(!exprWhere.isFalse()) { // оптимизация, важна так как во многих event'ах может учавствовать

            Result<SessionTableUsage> rUsedTable = new Result<>();
            try {
                if (where != null && writeFrom.mapHasPreread(context.getModifier()) && PropertyChange.needMaterializeWhere(exprWhere))
                    exprWhere = PropertyChange.materializeWhere("setmwh", where, session, innerKeys, innerValues, innerExprs, exprWhere, rUsedTable);

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

    public static <I extends PropertyInterface> PropertyMapImplement<?, I> getFullProperty(PropertyMapImplement<?, I> where, PropertyMapImplement<?, I> writeTo, PropertyInterfaceImplement<I> writeFrom) {
        MList<PropertyMapImplement<?, I>> mAnds = ListFact.mListMax(3);
        if(where != null) // optimization
            mAnds.add(where);
        mAnds.add(writeTo.mapChangeClassProperty());
        mAnds.add(writeFrom.mapChangeValueClassProperty(writeTo.property));
        return PropertyFact.createAnd(mAnds.immutableList().getCol());
    }

    @IdentityInstanceLazy
    private <T extends PropertyInterface> PropertyMapImplement<?, T> getTrueProperty() { // to avoid property leaks
        return PropertyFact.createTrue();
    }

    protected PropertyMapImplement<?, I> calcGroupWhereProperty() {
        return getFullProperty(where, writeTo, writeFrom);
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

        return ForAction.pushFor(innerInterfaces, where, mapInterfaces, mapping, context, push, orders, ordersNotNull, (context1, where, orders1, ordersNotNull1, mapInnerInterfaces) -> PropertyFact.createSetAction(context1, writeTo.map(mapInnerInterfaces), writeFrom.map(mapInnerInterfaces), where, orders1, ordersNotNull1, SetFact.EMPTYORDER()));
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // need this for property breakpoints
    }

    @Override
    protected AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, ImSet<Action<?>> recursiveAbstracts) {
        if(where == null) {
            assert getExtendInterfaces().isEmpty();
            // it can be mapped because of the assertion mapInterfaces.values + writeTo.values contains all inner interfaces
            AsyncMapChange<?, I> asyncChange = writeFrom.mapAsyncChange(writeTo, null);
            if(asyncChange != null)
                return asyncChange.map(mapInterfaces.reverse());
        }
        return null;
    }
}
