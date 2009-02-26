package platform.server.view.form;

import platform.base.BaseUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.io.DataOutputStream;
import java.io.IOException;

// считанные данные (должен быть интерфейс Serialize)
public class ReportData {

    private List<ReportRow> rows = new ArrayList<ReportRow>();
    public void add(Map<ObjectImplement,Integer> keys,Map<PropertyView,Object> properties) {
        rows.add(new ReportRow(keys,properties));
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeBoolean(rows.size()==0);
        if(rows.size()==0) return;

        ReportRow firstRow = rows.iterator().next();

        Set<ObjectImplement> objects = firstRow.keys.keySet();
        outStream.writeInt(objects.size());
        for(ObjectImplement object : objects) {
            outStream.writeUTF(object.getSID());
            outStream.writeInt(object.ID);
        }

        Set<PropertyView> properties = firstRow.values.keySet();
        outStream.writeInt(properties.size());
        for(PropertyView propertyView : properties) {
            outStream.writeUTF(propertyView.getSID());
            outStream.writeInt(propertyView.ID);
        }

        outStream.writeInt(rows.size());
        for(ReportRow row : rows)
            row.serialize(outStream);
    }
}

class ReportRow {
    Map<ObjectImplement,Integer> keys;
    Map<PropertyView,Object> values;

    ReportRow(Map<ObjectImplement, Integer> iKeys, Map<PropertyView, Object> iProperties) {
        keys = iKeys;
        values = iProperties;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for(Map.Entry<ObjectImplement,Integer> key : keys.entrySet()) {
            outStream.writeInt(key.getKey().ID);
            outStream.writeInt(key.getValue());
        }
        for(Map.Entry<PropertyView,Object> property : values.entrySet()) {
            outStream.writeInt(property.getKey().ID);
            BaseUtils.serializeObject(outStream,property.getValue());
        }
    }
}