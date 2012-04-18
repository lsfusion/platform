package platform.server.logics.property.actions.flow;

import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class ForActionProperty<I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    private final PropertyMapImplement<?, I> ifProp; // calculate
    private final OrderedMap<PropertyInterfaceImplement<I>, Boolean> orders; // calculate
    private final PropertyMapImplement<ClassPropertyInterface, I> action; // action
    private final PropertyMapImplement<ClassPropertyInterface, I> elseAction; // action
    private final boolean recursive;

    public ForActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, PropertyMapImplement<?, I> ifProp, OrderedMap<PropertyInterfaceImplement<I>, Boolean> orders, PropertyMapImplement<ClassPropertyInterface, I> action, PropertyMapImplement<ClassPropertyInterface, I> elseAction, boolean recursive) {
        super(sID, caption, innerInterfaces, mapInterfaces, merge(orders.keySet(), toList(ifProp, action)));

        assert elseAction == null || !recursive;

        this.ifProp = ifProp;
        this.orders = orders;
        this.action = action;
        this.elseAction = elseAction;
        this.recursive = recursive;

        finalizeInit();
    }

    public Set<Property> getChangeProps() {
        Set<Property> result = ((ActionProperty) action.property).getChangeProps();
        if(elseAction != null)
            result = mergeSet(result, ((ActionProperty) elseAction.property).getChangeProps());
        return result;
    }

    public Set<Property> getUsedProps() {
        Set<Property> result = new HashSet<Property>(((ActionProperty) action.property).getUsedProps());
        if(elseAction != null)
            result.addAll(((ActionProperty) elseAction.property).getUsedProps());
        ifProp.mapFillDepends(result);
        for(PropertyInterfaceImplement<I> order : orders.keySet())
            order.mapFillDepends(result);
        return result;
    }

    @Override
    protected FlowResult flowExecute(ExecutionContext context) throws SQLException {
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
                FlowResult actionResult = execute(context, action, row);
                if (actionResult != FlowResult.FINISH) {
                    if (actionResult != FlowResult.BREAK) {
                        result = actionResult;
                    }
                    break RECURSIVE;
                }
            }
        } while (recursive && !rows.isEmpty());

        if (execElse) {
            return execute(context, elseAction, crossJoin(mapInterfaces, context.getKeys()));
        }

        return result;
    }

    private Set<Map<I, DataObject>> readRows(DataSession session, Map<ClassPropertyInterface, DataObject> keys, Modifier modifier) throws SQLException {
        Query<I, PropertyInterfaceImplement<I>> query = new Query<I, PropertyInterfaceImplement<I>>(innerInterfaces, crossJoin(mapInterfaces, keys));
        Map<I,Expr> mapExprs = query.getMapExprs();

        query.and(ifProp.mapExpr(mapExprs, modifier).getWhere());
        for (PropertyInterfaceImplement<I> order : orders.keySet()) {
            query.properties.put(order, order.mapExpr(mapExprs, modifier));
        }
        return query.executeClasses(session, orders).keySet();
    }
}
