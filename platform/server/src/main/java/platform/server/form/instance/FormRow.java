package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImMap;

import java.io.DataOutputStream;
import java.io.IOException;

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
