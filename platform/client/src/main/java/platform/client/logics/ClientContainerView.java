package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientContainerView extends ClientComponentView {

    // идентификатор контейнера для общения с удаленной формой
    private int ID;

    public int getID() {
        return ID;
    }

    // заголовок контейнера
    private String title;

    public String getTitle() {
        return title;
    }

    public ClientContainerView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        ID = inStream.readInt();
        if(!inStream.readBoolean())
            title = inStream.readUTF();
    }
}
