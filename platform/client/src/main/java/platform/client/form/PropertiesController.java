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
    private Map<ClientPropertyDraw, Map<ClientGroupObject, List<ClientGroupObjectValue>>> groupColumnKeys = new HashMap<ClientPropertyDraw, Map<ClientGroupObject, List<ClientGroupObjectValue>>>();

    public void setColumnKeys(ClientPropertyDraw property,
                              Map<ClientGroupObject, List<ClientGroupObjectValue>> groupColumnKeys) {
        if (groupColumnKeys == null || groupColumnKeys.isEmpty()) {
            return;
        }

        Map<ClientGroupObject, List<ClientGroupObjectValue>> keys = this.groupColumnKeys.get(property);
        if (keys == null) {
            keys = new OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>>();
            //сразу вставляем все ключи, чтобы сохранить порядок
            for (ClientGroupObject columnGroup : property.columnGroupObjects) {
                keys.put(columnGroup, null);
            }

            this.groupColumnKeys.put(property, keys);
        }

        keys.putAll(groupColumnKeys);

        //находим декартово произведение ключей колонок
        List<ClientGroupObjectValue> propColumnKeys = new ArrayList<ClientGroupObjectValue>();
        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : keys.entrySet()) {
            List<ClientGroupObjectValue> groupObjectKeys = entry.getValue();

            if (propColumnKeys.size() == 0) {
                for (ClientGroupObjectValue groupObjectKey : groupObjectKeys) {
                    propColumnKeys.add(new ClientGroupObjectValue(groupObjectKey));
                }
            } else {
                List<ClientGroupObjectValue> newPropColumnKeys = new ArrayList<ClientGroupObjectValue>();
                for (ClientGroupObjectValue propColumnKey : propColumnKeys) {
                    for (ClientGroupObjectValue groupObjectKey : groupObjectKeys) {
                        newPropColumnKeys.add(new ClientGroupObjectValue(propColumnKey, groupObjectKey));
                    }
                }
                propColumnKeys = newPropColumnKeys;
            }
        }

        columnKeys.put(property, propColumnKeys);
    }

    public void setDisplayPropertiesValues(Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> pcolumnDisplayValues) {
        columnDisplayValues.putAll( pcolumnDisplayValues );
    }
}
