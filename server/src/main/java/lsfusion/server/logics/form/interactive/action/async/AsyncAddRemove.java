package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncAddRemove extends AsyncFormExec {
    public ObjectEntity object;
    public boolean add;

    public AsyncAddRemove(ObjectEntity object, boolean add) {
        this.object = object;
        this.add = add;
    }

    @Override
    public byte getTypeId() {
        return 4;
    }

    @Override
    public void serialize(DataOutputStream dataOutputStream) throws IOException {
        super.serialize(dataOutputStream);

        dataOutputStream.writeInt(object.getID());
        dataOutputStream.writeBoolean(add);
    }

    // should correspond ClientPushAsyncAdd.serialize
    @Override
    public PushAsyncResult deserializePush(DataInputStream inStream) throws IOException {
        assert add;
        return new PushAsyncAdd(new DataObject(inStream.readLong(), ((CustomClass)object.baseClass).getBaseClass().unknown));
    }
}