package lsfusion.server.form.instance;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.server.remote.FormReportInterface;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 21.10.2010
 * Time: 15:46:31
 */

public class ReportData<Order, Obj extends Order, PropertyReader> {
    private final List<Obj> keys;
    private final List<Pair<String, PropertyReader>> properties;
    private final Map<Obj, Integer> keyToIndex;
    private final List<List<Object>> keyRows = new ArrayList<>();
    private final List<List<Object>> propRows = new ArrayList<>();

    public ReportData(List<Obj> keys, List<Pair<String, PropertyReader>> properties) {
        this.keys = keys;
        keyToIndex = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            keyToIndex.put(keys.get(i), i);
        }
        this.properties = properties;
    }

    public void add(List<Object> keyValues, List<Object> propValues) {
        assert this.keys.size() == keyValues.size();
        assert this.properties.size() == propValues.size();
        keyRows.add(keyValues);
        propRows.add(propValues);
    }

    public Object getKeyValue(int row, ObjectInstance obj) {
        int objIndex = index(obj);
        return keyRows.get(row).get(objIndex);
    }

    private int index(ObjectInstance obj) {
        return keyToIndex.get(obj);
    }

    public int getRowCount() { return keyRows.size(); }

    public void serialize(DataOutputStream outStream, boolean custom, FormReportInterface<?, ?, ?, ?, Order, Obj, PropertyReader> formInterface) throws IOException {
        outStream.writeBoolean(keyRows.size() == 0);
        if (keyRows.size() == 0 && !custom) return;

        outStream.writeInt(keys.size());
        for(Obj object : keys) {
            outStream.writeUTF(formInterface.getObjectSID(object));
            outStream.writeInt(formInterface.getObjectID(object));
        }

        outStream.writeInt(properties.size());
        for(Pair<String, PropertyReader> propertyData : properties) {
            outStream.writeUTF(propertyData.first);
            outStream.writeInt(formInterface.getTypeID(propertyData.second));
            outStream.writeInt(formInterface.getID(propertyData.second));
            PropertyType type = formInterface.getPropertyType(propertyData.second);
            outStream.writeUTF(type == null ? "" : type.type);
            outStream.writeUTF(type == null ? "" : type.toDraw);
            outStream.writeInt(type == null ? 0 : type.length);
            outStream.writeInt(type == null ? 0 : type.precision);
        }

        outStream.writeInt(keyRows.size());
        for (int i = 0; i < keyRows.size(); i++) {
            for (Object obj : keyRows.get(i)) {
                BaseUtils.serializeObject(outStream, obj);
            }
            for (Object obj : propRows.get(i)) {
                BaseUtils.serializeObject(outStream, obj);
            }
        }
    }
}
