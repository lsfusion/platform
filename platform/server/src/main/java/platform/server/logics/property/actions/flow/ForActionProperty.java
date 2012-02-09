package platform.server.logics.property.actions.flow;

import platform.base.OrderedMap;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static platform.base.BaseUtils.*;

public class ForActionProperty<I extends PropertyInterface> extends ExtendContextActionProperty<I> {

    private final PropertyMapImplement<?, I> ifProp; // calculate
    private final OrderedMap<PropertyInterfaceImplement<I>, Boolean> orders; // calculate
    private final PropertyMapImplement<ClassPropertyInterface, I> action; // action
    private final boolean recursive;

    public ForActionProperty(String sID, String caption, Collection<I> innerInterfaces, List<I> mapInterfaces, PropertyMapImplement<?, I> ifProp, OrderedMap<PropertyInterfaceImplement<I>, Boolean> orders, PropertyMapImplement<ClassPropertyInterface, I> action, boolean recursive) {
        super(sID, caption, innerInterfaces, mapInterfaces, merge(orders.keySet(), toList(ifProp, action)));
        this.ifProp = ifProp;
        this.orders = orders;
        this.action = action;
        this.recursive = recursive;
    }

    public void execute(ExecutionContext context) throws SQLException {
        while(true) {
            Set<Map<I,DataObject>> rows = readRows(context.getSession(), context.getKeys(), context.getModifier());
            for(Map<I, DataObject> row : rows)
                execute(action, row, context);
            if(!recursive || rows.size()==0)
                return;
        }
    }

    private Set<Map<I, DataObject>> readRows(DataSession session, Map<ClassPropertyInterface, DataObject> keys, Modifier modifier) throws SQLException {
        Query<I, PropertyInterfaceImplement<I>> query = new Query<I, PropertyInterfaceImplement<I>>(innerInterfaces);
        query.putKeyWhere(crossJoin(mapInterfaces, keys));
        query.and(ifProp.mapExpr(query.mapKeys, modifier).getWhere());
        for(PropertyInterfaceImplement<I> order : orders.keySet())
            query.properties.put(order, order.mapExpr(query.mapKeys, modifier));
        return query.executeClasses(session, orders).keySet();
    }
}
