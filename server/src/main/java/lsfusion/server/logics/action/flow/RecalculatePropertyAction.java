package lsfusion.server.logics.action.flow;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class RecalculatePropertyAction<P extends PropertyInterface, W extends PropertyInterface, I extends PropertyInterface> extends ExtendContextAction<I> {
    private final PropertyMapImplement<P, I> recalc;
    private final PropertyInterfaceImplement<I> where;

    public RecalculatePropertyAction(LocalizedString caption, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces, PropertyMapImplement<P, I> recalc, PropertyMapImplement<W, I> where) {
        super(caption, innerInterfaces, mapInterfaces);
        this.recalc = recalc;
        this.where = where;
        assert recalc.property instanceof AggregateProperty; 
        finalizeInit();
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        ImList<PropertyInterfaceImplement<PropertyInterface>> listWheres = ListFact.EMPTY();
        return PropertyFact.createUnion(interfaces, listWheres);
    }

    @IdentityInstanceLazy
    private PropertyMapImplement<?, I> getFullProperty() {
        return where != null ? PropertyFact.createNotNull(where) : PropertyFact.createTrue();
    }

    @Override
    protected PropertyMapImplement<?, I> calcGroupWhereProperty() {
        return getFullProperty();
    }

    @Override
    public ImSet<Action> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException {
        PropertyChange<P> propertyChange = null;
        if(where != null) {
            Where exprWhere = where.mapExpr(innerExprs, context.getModifier()).getWhere();
            ImMap<P, DataObject> writeInnerValues = DataObject.onlyDataObjects(recalc.mapping.innerJoin(innerValues));
            if (writeInnerValues != null)
                propertyChange = new PropertyChange<>(writeInnerValues, recalc.mapping.rightJoin(innerKeys), ValueExpr.get(exprWhere), exprWhere);                
            else
                proceedNullException();
        }

        context.getDbManager().runAggregationRecalculation(context.getSession(), context.getSession().sql, (AggregateProperty<P>) recalc.property, propertyChange, true, false);
        return FlowResult.FINISH;
    }
}