package lsfusion.server.logics.form.interactive.instance;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;

import java.io.DataOutputStream;
import java.io.IOException;

@Deprecated
public class FormData {

    public final ImOrderSet<FormRow> rows;

    public FormData(ImOrderMap<ImMap<ObjectInstance,Object>, ImMap<PropertyDrawInstance,Object>> rows) {
        this.rows = rows.mapOrderSetValues(FormRow::new);
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeBoolean(rows.size()==0);
        if(rows.size()==0) return;

        FormRow firstRow = rows.iterator().next();

        ImSet<ObjectInstance> objects = firstRow.keys.keys();
        outStream.writeInt(objects.size());
        for(ObjectInstance object : objects) {
            outStream.writeUTF(object.getSID());
            outStream.writeInt(object.getID());
        }

        ImSet<PropertyDrawInstance> properties = firstRow.values.keys();
        outStream.writeInt(properties.size());
        for(PropertyDrawInstance propertyView : properties) {
            outStream.writeUTF(propertyView.getSID());
            outStream.writeInt(propertyView.getID());
        }

        outStream.writeInt(rows.size());
        for(FormRow row : rows)
            row.serialize(outStream);
    }
}
