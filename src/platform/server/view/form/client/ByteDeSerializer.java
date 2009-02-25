package platform.server.view.form.client;

import platform.server.view.form.GroupObjectValue;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.Filter;
import platform.server.view.form.RemoteForm;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class ByteDeSerializer {
    public static GroupObjectValue deserializeGroupObjectValue(byte[] state, GroupObjectImplement groupObject) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return new GroupObjectValue(dataStream, groupObject);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static Filter deserializeFilter(byte[] state, RemoteForm remoteForm) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return new Filter(dataStream, remoteForm);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }
}
