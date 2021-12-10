package lsfusion.client.form.property.cell;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.object.ClientGroupObjectValue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

public class ClientAsync implements Serializable {
    public final String displayString;
    public final String rawString;

    public final ClientGroupObjectValue key;

    public static final ClientAsync RECHECK = new ClientAsync("RECHECK", "RECHECK", null);
    public static final ClientAsync CANCELED = new ClientAsync("CANCELED", "CANCELED", null);

    private ClientAsync(String displayString, String rawString, ClientGroupObjectValue key) {
        this.displayString = displayString;
        this.rawString = rawString;
        this.key = key;
    }

    public ClientAsync(DataInputStream inStream, ClientForm form) throws IOException {
        displayString = inStream.readUTF();
        rawString = inStream.readUTF();
        if(inStream.readBoolean())
            key = new ClientGroupObjectValue(inStream, form);
        else
            key = null;
    }

    public static ClientAsync[] deserialize(byte[] asyncs, ClientForm form) {
        if(asyncs == null)
            return null;

        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(asyncs));
        try {
            ClientAsync[] result = new ClientAsync[inStream.readInt()];
            for(int i=0;i<result.length;i++)
                result[i] = new ClientAsync(inStream, form);
            return result;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ClientAsync && displayString.equals(((ClientAsync) o).displayString) && rawString.equals(((ClientAsync) o).rawString) && BaseUtils.nullEquals(key, ((ClientAsync) o).key);
    }

    @Override
    public int hashCode() {
        return 31 * (displayString.hashCode() * 31 + rawString.hashCode()) + BaseUtils.nullHash(key);
    }

    @Override
    public String toString() {
        return "<html>" + displayString + "</html>";
    }
}
