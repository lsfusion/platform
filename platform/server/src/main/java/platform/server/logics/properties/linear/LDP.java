package platform.server.logics.properties.linear;

import platform.server.logics.ObjectValue;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LDP<D extends PropertyInterface> extends LP<DataPropertyInterface, DataProperty<D>> {

    public LDP(DataProperty<D> iProperty) {super(iProperty);}

    public void changeProperty(DataSession session, Object value, Integer... iParams) throws SQLException {
        Map<DataPropertyInterface,ObjectValue> keys = new HashMap<DataPropertyInterface, ObjectValue>();
        Integer intNum = 0;
        for(int i : iParams) {
            DataPropertyInterface propertyInterface = listInterfaces.get(intNum);
            keys.put(propertyInterface,new ObjectValue(i,propertyInterface.interfaceClass));
            intNum++;
        }

        property.changeProperty(keys, value, false, session, null);
    }

    public void putNotNulls(Map<DataProperty, Set<DataPropertyInterface>> propNotNulls,Integer... iParams) {
        Set<DataPropertyInterface> interfaceNotNulls = new HashSet<DataPropertyInterface>();
        for(Integer iInterface : iParams)
            interfaceNotNulls.add(listInterfaces.get(iInterface));

        propNotNulls.put(property,interfaceNotNulls);
    }
}
