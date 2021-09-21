package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.input.InputResult;

import java.io.*;

public class AsyncChange extends AsyncInputExec {
    public DataClass changeType;
    public LP targetProp;

    public InputList inputList;

    public AsyncChange(DataClass changeType, LP targetProp, InputList inputList) {
        this.changeType = changeType;
        this.targetProp = targetProp;
        this.inputList = inputList;
    }

    @Override
    public byte getTypeId() {
        return 2;
    }

    @Override
    public void serialize(DataOutputStream dataOutputStream) throws IOException {
        super.serialize(dataOutputStream);

        TypeSerializer.serializeType(dataOutputStream, changeType);
        dataOutputStream.writeBoolean(inputList != null);
        if(inputList != null)
            AsyncSerializer.serializeInputList(inputList, dataOutputStream);
    }

    // should correspond ClientPushAsyncChange.serialize
    @Override
    public PushAsyncResult deserializePush(DataInputStream inStream) throws IOException {
        return new PushAsyncChange(InputResult.get(BaseUtils.readObject(inStream), changeType));
    }
}