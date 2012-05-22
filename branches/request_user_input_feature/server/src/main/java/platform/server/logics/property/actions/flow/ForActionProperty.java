package platform.server.logics.property.actions.flow;

import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class ForActionProperty<I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    private final CalcPropertyMapImplement<?, I> ifProp; // calculate
    private final OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders; // calculate
    private final ActionPropertyMapImplement<?, I> action; // action
    private final ActionPropertyMapImplement<?, I> elseAction; // action
    private final boolean recursive;

    public ForActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, CalcPropertyMapImplement<?, I> ifProp, OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, ActionPropertyMapImplement<?, I> action, ActionPropertyMapImplement<?, I> elseAction, boolean recursive) {
        super(sID, caption, innerInterfaces, mapInterfaces);

        assert elseAction == null || !recursive;

        this.ifProp = ifProp;
        this.orders = orders;
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
    public FlowResult execute(ExecutionContext<PropertyInterface> context) throws SQLException {
        FlowResult result = FlowResult.FINISH;

        boolean execElse = elseAction != null;

        Set<Map<I, DataObject>> rows;
        RECURSIVE:
        do {
            rows = readRows(context.getSession(), context.getKeys(), context.getModifier());
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

    private Set<Map<I, DataObject>> readRows(DataSession session, Map<PropertyInterface, DataObject> keys, Modifier modifier) throws SQLException {
        Query<I, CalcPropertyInterfaceImplement<I>> query = new Query<I, CalcPropertyInterfaceImplement<I>>(innerInterfaces, crossJoin(mapInterfaces, keys));
        Map<I,Expr> mapExprs = query.getMapExprs();

        query.and(ifProp.mapExpr(mapExprs, modifier).getWhere());
        for (CalcPropertyInterfaceImplement<I> order : orders.keySet()) {
            query.properties.put(order, order.mapExpr(mapExprs, modifier));
        }
        return query.executeClasses(session, orders).keySet();
    }

    protected CalcPropertyMapImplement<?, I> getGroupWhereProperty() {
        return DerivedProperty.createIfElseUProp(innerInterfaces, ifProp,
                action.mapWhereProperty(), elseAction != null ? elseAction.mapWhereProperty() : null, false);
    }
}
