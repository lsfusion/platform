package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.AggrExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ActionEvent<P extends PropertyInterface> extends Event<P, ActionProperty<P>> {

    public final boolean session; // session event
    private final OrderedMap<CalcPropertyInterfaceImplement<P>, Boolean> orders;
    private final boolean ordersNotNull;
    
    public ActionEvent(ActionProperty<P> writeTo, CalcPropertyMapImplement<?, P> where, OrderedMap<CalcPropertyInterfaceImplement<P>, Boolean> orders, boolean ordersNotNull, boolean session, int options) {
        super(writeTo, where);
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;
        this.session = session;
        this.options = options;
    }

    public final static int RESOLVE = 1; // обозначает что where - SET или DROP свойства, и выполнение этого event'а не имеет смысла
    private final int options;

    public <T extends PropertyInterface> void resolve(DataSession session) throws SQLException {
        if((options & RESOLVE)==0)
            return;

        PropertyChanges changes = session.getPropertyChanges();
        for(SessionCalcProperty<T> sessionCalcProperty : where.property.getSessionCalcDepends())
            if(sessionCalcProperty instanceof ChangedProperty)
                changes = changes.add(new PropertyChanges(sessionCalcProperty, ((ChangedProperty<T>)sessionCalcProperty).getFullChange(session)));
        new ExecutionEnvironment(session).execute(writeTo, getChange(changes), null);
    }
    
    private OrderedMap<Expr, Boolean> getOrderImplements(Map<P, KeyExpr> mapKeys, PropertyChanges changes) {
        OrderedMap<Expr, Boolean> result = new OrderedMap<Expr, Boolean>();
        for(Map.Entry<CalcPropertyInterfaceImplement<P>, Boolean> order : orders.entrySet())
            result.put(order.getKey().mapExpr(mapKeys, changes), order.getValue());
        return result;
    }

    public PropertySet<P> getChange(PropertyChanges changes) {
        Map<P,KeyExpr> mapKeys = writeTo.getMapKeys();
        return new PropertySet<P>(new HashMap<P, DataObject>(), mapKeys, where.mapExpr(mapKeys, changes).getWhere(), getOrderImplements(mapKeys, changes), ordersNotNull);
    }
}
