package lsfusion.server.logics.navigator.changed;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.server.logics.navigator.PropertyNavigator;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.serializeObject;

public class NavigatorChanges {

    ImMap<PropertyNavigator, Object> properties;

    public NavigatorChanges(ImMap<PropertyNavigator, Object> properties) {
        this.properties = properties;
    }

    public byte[] serialize() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serialize(new DataOutputStream(outStream));
            return outStream.toByteArray();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(properties.size());
        for (int i = 0, size = properties.size(); i < size; i++) {
            PropertyNavigator propertyNavigator = properties.getKey(i);
            Object value = properties.getValue(i);

            propertyNavigator.serialize(outStream);
            propertyNavigator.serializeValue(outStream, value);
        }
    }
}