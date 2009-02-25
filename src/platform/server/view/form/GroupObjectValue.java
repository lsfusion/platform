package platform.server.view.form;

import java.util.Map;
import java.io.DataInputStream;
import java.io.IOException;

public class GroupObjectValue extends GroupObjectMap<Integer> {

    public GroupObjectValue() {}
    GroupObjectValue(Map<ObjectImplement,Integer> iValue) {
        putAll(iValue);
    }

    public GroupObjectValue(DataInputStream inStream,GroupObjectImplement groupObject) throws IOException {
        for (ObjectImplement object : groupObject)
            put(object, inStream.readInt());
    }
}
