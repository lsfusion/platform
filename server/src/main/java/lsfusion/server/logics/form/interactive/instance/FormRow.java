package lsfusion.server.logics.form.interactive.instance;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;

import java.io.DataOutputStream;
import java.io.IOException;

@Deprecated
public class FormRow {

    public ImMap<ObjectInstance,Object> keys;
    public ImMap<PropertyDrawInstance,Object> values;

    public FormRow(ImMap<ObjectInstance, Object> keys, ImMap<PropertyDrawInstance, Object> values) {
        this.keys = keys;
        this.values = values;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for(int i=0,size=keys.size();i<size;i++) {
            outStream.writeInt(keys.getKey(i).getID());
            BaseUtils.serializeObject(outStream,keys.getValue(i));
        }
        for(int i=0,size=values.size();i<size;i++) {
            outStream.writeInt(values.getKey(i).getID());
            BaseUtils.serializeObject(outStream,values.getValue(i));
        }
    }
}
