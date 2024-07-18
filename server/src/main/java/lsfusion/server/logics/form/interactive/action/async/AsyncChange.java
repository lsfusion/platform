package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class AsyncChange extends AsyncFormExec {
    public ImList<PropertyDrawEntity> properties;

    public Serializable value;

    public AsyncChange(ImList<PropertyDrawEntity> properties, Serializable value) {
        this.properties = properties;
        this.value = value;
    }

    @Override
    public void serialize(ConnectionContext context, DataOutputStream outStream) throws IOException {
        outStream.writeInt(properties.size());
        for(PropertyDrawEntity property : properties)
            outStream.writeInt(property.getID());
        FormChanges.serializeConvertFileValue(value, outStream, context);
    }

    @Override
    public byte getTypeId() {
        return 6;
    }
}
