package platform.client.form;

import platform.base.OrderedMap;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesController {
    protected Map<ClientPropertyDraw, List<ClientGroupObjectValue>> columnKeys = new HashMap<ClientPropertyDraw, List<ClientGroupObjectValue>>();
    protected Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnDisplayValues = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    public void setColumnKeys(ClientPropertyDraw property,
                              OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>> groupColumnKeys) {
        //находим декартово произведение ключей колонок
        List<ClientGroupObjectValue> propColumnKeys = new ArrayList<ClientGroupObjectValue>();
        propColumnKeys.add(new ClientGroupObjectValue());
        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : groupColumnKeys.entrySet()) {
            List<ClientGroupObjectValue> groupObjectKeys = entry.getValue();

            List<ClientGroupObjectValue> newPropColumnKeys = new ArrayList<ClientGroupObjectValue>();
            for (ClientGroupObjectValue propColumnKey : propColumnKeys) {
                for (ClientGroupObjectValue groupObjectKey : groupObjectKeys) {
                    newPropColumnKeys.add(new ClientGroupObjectValue(propColumnKey, groupObjectKey));
                }
            }
            propColumnKeys = newPropColumnKeys;
        }

        columnKeys.put(property, propColumnKeys);
    }

    public void setDisplayPropertiesValues(Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> pcolumnDisplayValues) {
        columnDisplayValues = pcolumnDisplayValues;
    }
}
