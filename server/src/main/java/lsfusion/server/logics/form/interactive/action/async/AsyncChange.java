package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.BaseUtils;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class AsyncChange extends AsyncFormExec {

    public PropertyDrawEntity property;

    public Serializable value;

    public AsyncChange(PropertyDrawEntity property, Serializable value) {
        this.property = property;
        this.value = value;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(property.getID());
        BaseUtils.serializeObject(value);
    }

    @Override
    public byte getTypeId() {
        return 5;
    }
}
