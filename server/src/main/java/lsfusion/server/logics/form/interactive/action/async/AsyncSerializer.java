package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.BaseUtils;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AsyncSerializer {

    public static void serializeEventExec(AsyncEventExec eventExec, DataOutputStream outStream) throws IOException {
        outStream.writeByte(eventExec == null ? 0 : eventExec.getTypeId());
        if(eventExec != null)
            eventExec.serialize(outStream);
    }

    public static byte[] serializeInputList(InputList inputList) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        serializeInputList(inputList, dataStream);

        return outStream.toByteArray();
    }

    public static void serializeInputList(InputList inputList, DataOutputStream dataStream) throws IOException {
        SerializationUtil.serializeArray(dataStream, inputList.actions);
        for(AsyncExec actionAsync : inputList.actionAsyncs)
            AsyncSerializer.serializeEventExec(actionAsync, dataStream);
        dataStream.writeBoolean(inputList.strict);
    }
}
