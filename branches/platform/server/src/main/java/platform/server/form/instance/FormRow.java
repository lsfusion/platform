package platform.server.form.instance;

import platform.base.BaseUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class FormRow {

    public Map<ObjectInstance,Object> keys;
    public Map<PropertyDrawInstance,Object> values;

    public FormRow(Map<ObjectInstance, Object> iKeys, Map<PropertyDrawInstance, Object> iProperties) {
        keys = iKeys;
        values = iProperties;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for(Map.Entry<ObjectInstance,Object> key : keys.entrySet()) {
            outStream.writeInt(key.getKey().getID());
            BaseUtils.serializeObject(outStream,key.getValue());
        }
        for(Map.Entry<PropertyDrawInstance,Object> property : values.entrySet()) {
            outStream.writeInt(property.getKey().getID());
            BaseUtils.serializeObject(outStream,property.getValue());
        }
    }
}
