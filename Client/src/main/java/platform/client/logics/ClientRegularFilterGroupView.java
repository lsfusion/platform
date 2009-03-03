package platform.client.logics;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClientRegularFilterGroupView extends ClientFunctionView {
    public int ID;
    public List<ClientRegularFilterView> filters = new ArrayList<ClientRegularFilterView>();

    public ClientRegularFilterGroupView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        ID = inStream.readInt();

        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            filters.add(new ClientRegularFilterView(inStream));
    }
}
