package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClientRegularFilterGroup extends ClientFunction {
    
    public int ID;
    public final List<ClientRegularFilter> filters = new ArrayList<ClientRegularFilter>();

    public int defaultFilter = -1;

    public ClientRegularFilterGroup(DataInputStream inStream, Collection<ClientContainer> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        ID = inStream.readInt();

        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            filters.add(new ClientRegularFilter(inStream));

        defaultFilter = inStream.readInt();
    }
}
