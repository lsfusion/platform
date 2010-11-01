package platform.server.form.instance;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

// считанные данные (должен быть интерфейс Serialize)
public class FormData {

    public List<FormRow> rows = new ArrayList<FormRow>();

    public void add(Map<ObjectInstance,Object> keys,Map<PropertyDrawInstance,Object> properties) {
        rows.add(new FormRow(keys,properties));
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeBoolean(rows.size()==0);
        if(rows.size()==0) return;

        FormRow firstRow = rows.iterator().next();

        Set<ObjectInstance> objects = firstRow.keys.keySet();
        outStream.writeInt(objects.size());
        for(ObjectInstance object : objects) {
            outStream.writeUTF(object.getsID());
            outStream.writeInt(object.getID());
        }

        Set<PropertyDrawInstance> properties = firstRow.values.keySet();
        outStream.writeInt(properties.size());
        for(PropertyDrawInstance propertyView : properties) {
            outStream.writeUTF(propertyView.getsID());
            outStream.writeInt(propertyView.getID());
        }

        outStream.writeInt(rows.size());
        for(FormRow row : rows)
            row.serialize(outStream);
    }
}
