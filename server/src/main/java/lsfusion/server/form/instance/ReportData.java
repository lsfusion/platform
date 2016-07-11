package lsfusion.server.form.instance;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;

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

public class ReportData {
    private final List<ObjectInstance> keys;
    private final List<Pair<String, PropertyReaderInstance>> properties;
    private final Map<ObjectInstance, Integer> keyToIndex;
    private final List<List<Object>> keyRows = new ArrayList<>();
    private final List<List<Object>> propRows = new ArrayList<>();

    public ReportData(List<ObjectInstance> keys, List<Pair<String, PropertyReaderInstance>> properties) {
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

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeBoolean(keyRows.size() == 0);
        if (keyRows.size() == 0) return;

        outStream.writeInt(keys.size());
        for(ObjectInstance object : keys) {
            outStream.writeUTF(object.getsID());
            outStream.writeInt(object.getID());
        }

        outStream.writeInt(properties.size());
        for(Pair<String, PropertyReaderInstance> propertyData : properties) {
            outStream.writeUTF(propertyData.first);
            outStream.writeInt(propertyData.second.getTypeID());
            outStream.writeInt(propertyData.second.getID());
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
