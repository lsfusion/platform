package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientContainer extends ClientComponent {

    // идентификатор контейнера для общения с удаленной формой
    private int ID;

    public int getID() {
        return ID;
    }

    public String title;
    public String description;

    public ClientContainer(DataInputStream inStream, Collection<ClientContainer> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        ID = inStream.readInt();
        if(!inStream.readBoolean())
            title = inStream.readUTF();
        if (!inStream.readBoolean())
            description = inStream.readUTF();
    }

    @Override
    public String toString() {
        String result = title == null ? "" : title;
        if (description == null)
            result += " (";
        else
            result += (result.isEmpty() ? "" : " ") + "(" + description + ",";
        result += ID;
        result += ")";
        return result;
    }
}
