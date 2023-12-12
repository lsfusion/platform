package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.input.InputResult;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;

import java.io.*;

public class AsyncInput extends AsyncFormExec {
    public final DataClass changeType;

    public final InputList inputList;
    public final InputListAction[] inputListActions;

    public final String customEditorFunction;

    public AsyncInput(DataClass changeType, InputList inputList, InputListAction[] inputListActions, String customEditorFunction) {
        this.changeType = changeType;
        this.inputList = inputList;
        this.inputListActions = inputListActions;
        this.customEditorFunction = customEditorFunction;
    }

    @Override
    public byte getTypeId() {
        return 3;
    }

    @Override
    public void serialize(ConnectionContext context, DataOutputStream dataOutputStream) throws IOException {
        super.serialize(context, dataOutputStream);

        TypeSerializer.serializeType(dataOutputStream, changeType);
        dataOutputStream.writeBoolean(inputList != null);
        if(inputList != null)
            AsyncSerializer.serializeInputList(inputList, dataOutputStream);

        dataOutputStream.writeBoolean(inputListActions != null);
        if(inputListActions != null)
            AsyncSerializer.serializeInputListActions(inputListActions, context, dataOutputStream);

        dataOutputStream.writeBoolean(customEditorFunction != null);
        if (customEditorFunction != null)
            dataOutputStream.writeUTF(customEditorFunction);
    }

    // should correspond ClientPushAsyncChange.serialize
    @Override
    public PushAsyncResult deserializePush(DataInputStream inStream) throws IOException {
        return new PushAsyncInput(InputResult.get(BaseUtils.readObject(inStream), changeType));
    }
}