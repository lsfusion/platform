package platform.client.logics;

import platform.client.logics.classes.ClientClass;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

public class ClientObject implements Serializable {

    private Integer ID = 0;

    public Integer getID() {
        return ID;
    }

    public String caption;
    public boolean addOnTransaction;

    // вручную заполняется
    public ClientGroupObject groupObject;

    public ClientObjectIDCell objectIDCell;

    public ClientClass baseClass;

    public ClientClassCell classCell;

    public ClientClassChooser classChooser;

    public ClientObject(DataInputStream inStream, Collection<ClientContainer> containers, ClientGroupObject iGroupObject) throws ClassNotFoundException, IOException {

        groupObject = iGroupObject;

        ID = inStream.readInt();
        caption = inStream.readUTF();
        addOnTransaction = inStream.readBoolean();

        baseClass = ClientClass.deserialize(inStream);

        objectIDCell = new ClientObjectIDCell(inStream, containers, this);
        classCell = new ClientClassCell(inStream, containers, this);
        classChooser = new ClientClassChooser(inStream,containers);
    }

    public String toString() { return objectIDCell.caption; }
}
