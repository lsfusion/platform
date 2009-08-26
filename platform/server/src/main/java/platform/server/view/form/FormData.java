package platform.server.view.form;

import platform.base.BaseUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// считанные данные (должен быть интерфейс Serialize)
public class FormData {

    private List<FormRow> rows = new ArrayList<FormRow>();
    public void add(Map<ObjectImplement,Object> keys,Map<PropertyView,Object> properties) {
        rows.add(new FormRow(keys,properties));
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeBoolean(rows.size()==0);
        if(rows.size()==0) return;

        FormRow firstRow = rows.iterator().next();

        Set<ObjectImplement> objects = firstRow.keys.keySet();
        outStream.writeInt(objects.size());
        for(ObjectImplement object : objects) {
            outStream.writeUTF(object.sID);
            outStream.writeInt(object.ID);
        }

        Set<PropertyView> properties = firstRow.values.keySet();
        outStream.writeInt(properties.size());
        for(PropertyView propertyView : properties) {
            outStream.writeUTF(propertyView.sID);
            outStream.writeInt(propertyView.ID);
        }

        outStream.writeInt(rows.size());
        for(FormRow row : rows)
            row.serialize(outStream);
    }
}

class FormRow {
    Map<ObjectImplement,Object> keys;
    Map<PropertyView,Object> values;

    FormRow(Map<ObjectImplement, Object> iKeys, Map<PropertyView, Object> iProperties) {
        keys = iKeys;
        values = iProperties;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for(Map.Entry<ObjectImplement,Object> key : keys.entrySet()) {
            outStream.writeInt(key.getKey().ID);
            BaseUtils.serializeObject(outStream,key.getValue());
        }
        for(Map.Entry<PropertyView,Object> property : values.entrySet()) {
            outStream.writeInt(property.getKey().ID);
            BaseUtils.serializeObject(outStream,property.getValue());
        }
    }
}