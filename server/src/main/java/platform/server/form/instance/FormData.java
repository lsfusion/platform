package platform.server.form.instance;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;

import java.io.DataOutputStream;
import java.io.IOException;

// считанные данные (должен быть интерфейс Serialize)
public class FormData {

    public final ImOrderSet<FormRow> rows;

    public FormData(ImOrderMap<ImMap<ObjectInstance,Object>, ImMap<PropertyDrawInstance,Object>> rows) {
        this.rows = rows.mapOrderSetValues(new GetKeyValue<FormRow, ImMap<ObjectInstance, Object>, ImMap<PropertyDrawInstance, Object>>() {
            public FormRow getMapValue(ImMap<ObjectInstance, Object> key, ImMap<PropertyDrawInstance, Object> value) {
                return new FormRow(key,value);
            }});
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeBoolean(rows.size()==0);
        if(rows.size()==0) return;

        FormRow firstRow = rows.iterator().next();

        ImSet<ObjectInstance> objects = firstRow.keys.keys();
        outStream.writeInt(objects.size());
        for(ObjectInstance object : objects) {
            outStream.writeUTF(object.getsID());
            outStream.writeInt(object.getID());
        }

        ImSet<PropertyDrawInstance> properties = firstRow.values.keys();
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
