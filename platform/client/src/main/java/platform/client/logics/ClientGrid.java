package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientGrid extends ClientComponent {

    public boolean showFind;
    public boolean showFilter;

    public byte minRowCount;
    public boolean tabVertical = true;
    public final boolean autoHide;

    public ClientGrid(DataInputStream inStream, Collection<ClientContainer> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        showFind = inStream.readBoolean();
        showFilter = inStream.readBoolean();

        minRowCount = inStream.readByte();
        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();
    }
}
