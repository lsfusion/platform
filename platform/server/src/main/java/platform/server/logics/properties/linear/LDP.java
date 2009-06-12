package platform.server.logics.properties.linear;

import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LDP extends LP<DataPropertyInterface, DataProperty> {

    public LDP(DataProperty iProperty) {super(iProperty);}

    public void putNotNulls(Map<DataProperty, Set<DataPropertyInterface>> propNotNulls,Integer... iParams) {
        Set<DataPropertyInterface> interfaceNotNulls = new HashSet<DataPropertyInterface>();
        for(Integer iInterface : iParams)
            interfaceNotNulls.add(listInterfaces.get(iInterface));

        propNotNulls.put(property,interfaceNotNulls);
    }
}
