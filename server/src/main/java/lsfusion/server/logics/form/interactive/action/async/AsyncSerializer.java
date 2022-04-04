package lsfusion.server.logics.form.interactive.action.async;

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
        dataStream.write(inputList.actions.length);
        for(InputListAction action : inputList.actions) {
            dataStream.writeUTF(action.action);
            dataStream.write(action.quickAccessList.size());
            for(QuickAccess quickAccess : action.quickAccessList) {
                dataStream.writeByte(serializeQuickAccessMode(quickAccess.mode));
                dataStream.writeBoolean(quickAccess.hover);
            }
        }
        for(AsyncExec actionAsync : inputList.actionAsyncs)
            AsyncSerializer.serializeEventExec(actionAsync, dataStream);
        dataStream.writeBoolean(inputList.strict);
    }

    public static int serializeQuickAccessMode(QuickAccessMode quickAccessMode) {
            if (quickAccessMode == null) {
                return 0;
            } else {
                switch (quickAccessMode) {
                    case ALL:
                        return 1;
                    case SELECTED:
                        return 2;
                    case FOCUSED:
                        return 3;
                }
            }
        throw new UnsupportedOperationException();
    }
}
