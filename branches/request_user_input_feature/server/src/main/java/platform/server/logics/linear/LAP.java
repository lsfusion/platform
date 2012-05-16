package platform.server.logics.linear;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.ActionClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.DataSession;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.List;

import static platform.server.logics.PropertyUtils.readCalcImplements;

public class LAP extends LP<ClassPropertyInterface, ActionProperty> {

    public LAP(ActionProperty property) {
        super(property);
    }

    public LAP(ActionProperty property, List<ClassPropertyInterface> listInterfaces) {
        super(property, listInterfaces);
    }

    public List<ClientAction> execute(DataSession session, DataObject... objects) throws SQLException {
        return property.execute(getMapValues(objects), new ExecutionEnvironment(session), null);
    }

    public <P extends PropertyInterface> void setEventAction(LCP<P> lp, Integer... mapping) {
        setEventAction(false, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventSetAction(LCP<P> lp, Integer... mapping) {
        setEventAction(true, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventAction(boolean changedSet, LCP<P> lp, Integer... mapping) {
        property.setEventAction(new CalcPropertyMapImplement<P, ClassPropertyInterface>(lp.property.getChanged(changedSet ? IncrementType.SET : IncrementType.LEFTCHANGE), lp.getMap(listInterfaces)), 0);
    }

    public <P extends PropertyInterface> void setEventAction(Object... params) {
        List<CalcPropertyInterfaceImplement<ClassPropertyInterface>> listImplements = readCalcImplements(listInterfaces, params);
        property.setEventAction((CalcPropertyMapImplement<?, ClassPropertyInterface>) listImplements.get(0), 0);
    }

    public ValueClass[] getMapClasses() {
        return BaseUtils.mapList(listInterfaces, property.getMapClasses()).toArray(new ValueClass[0]);
    }
}
