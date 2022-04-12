package lsfusion.server.logics.form.interactive.action.async;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.nvl;

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
        dataStream.write(inputList.actions.length);
        for(InputListAction action : inputList.actions) {
            dataStream.writeUTF(action.action);
            AsyncSerializer.serializeEventExec(action.asyncExec, dataStream);
            dataStream.writeUTF(nvl(action.keyStroke, ""));
            dataStream.write(action.quickAccessList.size());
            for(QuickAccess quickAccess : action.quickAccessList) {
                dataStream.writeByte(serializeQuickAccessMode(quickAccess.mode));
                dataStream.writeBoolean(quickAccess.hover);
            }
        }
        dataStream.writeBoolean(inputList.strict);
    }

    public static int serializeQuickAccessMode(QuickAccessMode quickAccessMode) {
        switch (quickAccessMode) {
            case ALL:
                return 0;
            case SELECTED:
                return 1;
            case FOCUSED:
                return 2;
        }
        throw new UnsupportedOperationException();
    }
}
