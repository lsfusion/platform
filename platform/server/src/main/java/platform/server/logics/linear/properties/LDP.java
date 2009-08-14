package platform.server.logics.linear.properties;

import platform.server.logics.properties.*;
import platform.server.logics.BusinessLogics;
import platform.base.BaseUtils;

import java.util.*;

public class LDP extends LP<DataPropertyInterface, DataProperty> {

    public LDP(DataProperty iProperty) {super(iProperty);}

    public void putNotNulls(Map<DataProperty, Set<DataPropertyInterface>> propNotNulls,Integer... iParams) {
        Set<DataPropertyInterface> interfaceNotNulls = new HashSet<DataPropertyInterface>();
        for(Integer iInterface : iParams)
            interfaceNotNulls.add(listInterfaces.get(iInterface));

        propNotNulls.put(property,interfaceNotNulls);
    }

    public <D extends PropertyInterface> void setDefProp(LP<D,?> defaultProperty, Object... params) {
        List<PropertyInterfaceImplement<DataPropertyInterface>> defImplements = BusinessLogics.readImplements(listInterfaces,params);
        property.defaultData = new DefaultData<D>(BusinessLogics.mapImplement(defaultProperty,defImplements.subList(0,defaultProperty.listInterfaces.size())),
                BaseUtils.<PropertyInterfaceImplement<DataPropertyInterface>, PropertyMapImplement<?, DataPropertyInterface>>immutableCast(defImplements.subList(defaultProperty.listInterfaces.size(), defImplements.size())));                    
    }
}
