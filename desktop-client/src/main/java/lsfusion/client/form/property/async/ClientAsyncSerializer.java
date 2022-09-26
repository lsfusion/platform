package lsfusion.client.form.property.async;

import lsfusion.client.form.property.cell.classes.controller.suggest.CompletionType;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientAsyncSerializer {

    public static ClientAsyncEventExec deserializeEventExec(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();
        switch (type) {
            case 0:
                return null;
            case 1:
                return new ClientAsyncOpenForm(inStream);
            case 2:
                return new ClientAsyncCloseForm(inStream);
            case 3:
                return new ClientAsyncInput(inStream);
            case 4:
                return new ClientAsyncAddRemove(inStream);
            case 5:
                return new ClientAsyncNoWaitExec();
            case 6:
                return new ClientAsyncChange(inStream);
        }
        throw new UnsupportedOperationException();
    }

    public static ClientInputList deserializeInputList(DataInputStream inStream) throws IOException {
        int actionsLength = inStream.readByte();
        ClientInputListAction[] actions = new ClientInputListAction[actionsLength];
        for (int i = 0; i < actionsLength; i++) {
            String action = inStream.readUTF();
            ClientAsyncEventExec asyncExec = deserializeEventExec(inStream);
            KeyStroke keyStroke = KeyStroke.getKeyStroke(SerializationUtil.readString(inStream));
            BindingMode editingBindingMode = BindingMode.deserialize(inStream);
            int quickAccessLength = inStream.readByte();
            List<ClientQuickAccess> quickAccessList = new ArrayList<>();
            for (int j = 0; j < quickAccessLength; j++) {
                quickAccessList.add(new ClientQuickAccess(deserializeQuickAccessMode(inStream), inStream.readBoolean()));
            }
            int index = inStream.readInt();
            actions[i] = new ClientInputListAction(action, asyncExec, keyStroke, editingBindingMode, quickAccessList, index);
        }

        return new ClientInputList(actions, inStream.readBoolean() ? CompletionType.STRICT : CompletionType.NON_STRICT, null);
    }

    public static ClientInputList deserializeInputList(byte[] array) throws IOException {
        if(array == null)
            return null;
        return deserializeInputList(new DataInputStream(new ByteArrayInputStream(array)));
    }

    public static ClientQuickAccessMode deserializeQuickAccessMode(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();
        switch (type) {
            case 0:
                return ClientQuickAccessMode.ALL;
            case 1:
                return ClientQuickAccessMode.SELECTED;
            case 2:
                return ClientQuickAccessMode.FOCUSED;
        }
        throw new UnsupportedOperationException();
    }
}
