package platform.server.view.form;

import platform.base.BaseUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class FormRow {

    public Map<ObjectImplement,Object> keys;
    public Map<PropertyView,Object> values;

    public FormRow(Map<ObjectImplement, Object> iKeys, Map<PropertyView, Object> iProperties) {
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
