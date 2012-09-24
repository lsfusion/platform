package platform.server.logics.linear;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static platform.server.logics.PropertyUtils.readCalcImplements;

public class LAP<T extends PropertyInterface> extends LP<T, ActionProperty<T>> {

    public LAP(ActionProperty<T> property) {
        super(property);
    }

    public LAP(ActionProperty<T> property, List<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public void execute(DataSession session, DataObject... objects) throws SQLException {
        property.execute(getMapValues(objects), session, null);
    }

    public FlowResult execute(ExecutionContext<?> context, DataObject... objects) throws SQLException {
        return property.execute(context.override(getMapValues(objects), null, null));
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, LCP<P> lp, Integer... mapping) {
        setEventAction(lm, false, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, boolean session, LCP<P> lp, Integer... mapping) {
        setEventAction(lm, false, session, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventSetAction(LogicsModule lm, LCP<P> lp, Integer... mapping) {
        setEventAction(lm, true, false, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, boolean changedSet, boolean session, LCP<P> lp, Integer... mapping) {
        setEventAction(lm, changedSet ? IncrementType.SET : IncrementType.LEFTCHANGE, session, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, IncrementType type, boolean session, LCP<P> lp, Integer... mapping) {
        Map<P,T> map = new HashMap<P, T>();
        for(int i=0;i<lp.listInterfaces.size();i++)
            map.put(lp.listInterfaces.get(i), listInterfaces.get(mapping[i]-1));
        lm.addEventAction(property, new CalcPropertyMapImplement<P, T>(lp.property.getChanged(type), map), new OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean>(), false, session, false);
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, boolean session, boolean descending, boolean ordersNotNull, Object... params) {
        List<CalcPropertyInterfaceImplement<T>> listImplements = readCalcImplements(listInterfaces, params);
        OrderedMap<CalcPropertyInterfaceImplement<T>, Boolean> orders = BaseUtils.toOrderedMap(listImplements.subList(1, listImplements.size()), descending);
        lm.addEventAction(property, (CalcPropertyMapImplement<?, T>) listImplements.get(0), orders, ordersNotNull, session, false);
    }

    public ValueClass[] getInterfaceClasses() {
        return BaseUtils.mapList(listInterfaces, property.getInterfaceClasses()).toArray(new ValueClass[0]);
    }

    public <U extends PropertyInterface> ActionPropertyMapImplement<T, U> getImplement(U... mapping) {
        return new ActionPropertyMapImplement<T, U>(property, getMap(mapping));
    }
}
