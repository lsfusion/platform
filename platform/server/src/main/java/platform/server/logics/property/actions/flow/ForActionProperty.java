package platform.server.logics.property.actions.flow;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.PropertySet;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class ForActionProperty<I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    private final CalcPropertyMapImplement<?, I> ifProp; // calculate
    private final OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders; // calculate
    private final boolean ordersNotNull;
    private final ActionPropertyMapImplement<?, I> action; // action
    private final ActionPropertyMapImplement<?, I> elseAction; // action
    private final boolean recursive;

    public ForActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, CalcPropertyMapImplement<?, I> ifProp, OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, ActionPropertyMapImplement<?, I> action, ActionPropertyMapImplement<?, I> elseAction, boolean recursive) {
        super(sID, caption, innerInterfaces, mapInterfaces);

        assert elseAction == null || !recursive;

        this.ifProp = ifProp;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;
        this.action = action;
        this.elseAction = elseAction;
        this.recursive = recursive;

        finalizeInit();
    }

    public Set<ActionProperty> getDependActions() {
        Set<ActionProperty> result = Collections.singleton((ActionProperty) action.property);
        if(elseAction != null)
            result = addSet(result, elseAction.property);
        return result;
    }

    @Override
    public Set<CalcProperty> getUsedProps() {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        ifProp.mapFillDepends(result);
        for(CalcPropertyInterfaceImplement<I> order : orders.keySet())
            order.mapFillDepends(result);
        result.addAll(super.getUsedProps());
        return result;
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException {
        FlowResult result = FlowResult.FINISH;

        boolean execElse = elseAction != null;

        Collection<Map<I, DataObject>> rows;
        RECURSIVE:
        do {
            rows = readRows(context);
            if (!rows.isEmpty()) {
                execElse = false;
            }
            for (Map<I, DataObject> row : rows) {
                FlowResult actionResult = execute(context, action, row, mapInterfaces);
                if (actionResult != FlowResult.FINISH) {
                    if (actionResult != FlowResult.BREAK) {
                        result = actionResult;
                    }
                    break RECURSIVE;
                }
            }
        } while (recursive && !rows.isEmpty());

        if (execElse) {
            return execute(context, elseAction, crossJoin(mapInterfaces, context.getKeys()), mapInterfaces);
        }

        return result;
    }

    private Collection<Map<I, DataObject>> readRows(ExecutionContext<PropertyInterface> context) throws SQLException {

        Map<I, DataObject> mapValues = crossJoin(mapInterfaces, context.getKeys());
        Map<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(BaseUtils.filterNot(innerInterfaces, mapValues.keySet()));
        Map<I, Expr> mapExprs = BaseUtils.merge(mapKeys, DataObject.getMapExprs(mapValues));

        OrderedMap<Expr, Boolean> orderExprs = new OrderedMap<Expr, Boolean>();
        for (Map.Entry<CalcPropertyInterfaceImplement<I>, Boolean> order : orders.entrySet())
            orderExprs.put(order.getKey().mapExpr(mapExprs, context.getModifier()), order.getValue());

        return new PropertySet<I>(mapValues, mapKeys, ifProp.mapExpr(mapExprs, context.getModifier()).getWhere(), orderExprs, ordersNotNull).executeClasses(context.getEnv());
    }

    protected CalcPropertyMapImplement<?, I> getGroupWhereProperty() {
        CalcPropertyMapImplement<?, I> whereProp = ifProp;
        if(ordersNotNull)
            whereProp = DerivedProperty.createAnd(innerInterfaces, ifProp, orders.keySet());
        return DerivedProperty.createIfElseUProp(innerInterfaces, whereProp,
                action.mapWhereProperty(), elseAction != null ? elseAction.mapWhereProperty() : null, false);
    }
}
