package lsfusion.server.logics.navigator.changed;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.navigator.ImageElementNavigator;
import lsfusion.server.logics.navigator.NavigatorElement;
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

    public byte[] serialize(ConnectionContext context) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serialize(context, new DataOutputStream(outStream));
            return outStream.toByteArray();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void serialize(ConnectionContext context, DataOutputStream outStream) throws IOException {
        outStream.writeInt(properties.size());
        for (int i = 0, size = properties.size(); i < size; i++) {
            PropertyNavigator propertyNavigator = properties.getKey(i);
            Object value = properties.getValue(i);

            propertyNavigator.serialize(outStream);

            FormChanges.serializeConvertFileValue(outStream, getNeedImage(propertyNavigator), value, context);
        }
    }

    private static FormChanges.NeedImage getNeedImage(PropertyNavigator reader) {
        if (reader instanceof ImageElementNavigator) {
            NavigatorElement element = ((ImageElementNavigator) reader).getElement();
            return new FormChanges.NeedImage(reader.getProperty().getType(), imagePath -> AppServerImage.createNavigatorImage(imagePath, element));
        }
        return null;
    }
}