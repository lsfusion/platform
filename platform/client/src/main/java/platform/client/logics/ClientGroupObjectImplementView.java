package platform.client.logics;

import platform.base.DefaultIDGenerator;
import platform.base.IDGenerator;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class ClientGroupObjectImplementView extends ArrayList<ClientObjectImplementView>
                                 implements Serializable {

    private Integer ID = 0;

    public Integer getID() {
        return ID;
    }

    public byte banClassView = 0;

    public ClientGridView gridView;
    public ClientShowTypeView showTypeView;

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (this.ID != null ? this.ID.hashCode() : 0);
        return hash;
    }

    public ClientGroupObjectImplementView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        ID = inStream.readInt();
        banClassView = inStream.readByte();

        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            add(new ClientObjectImplementView(inStream,containers,this));

        gridView = new ClientGridView(inStream, containers);
        showTypeView = new ClientShowTypeView(inStream, containers);
    }

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private String actionID = null;
    public String getActionID() {
        if(actionID==null)
            actionID = "changeGroupObject" + idGenerator.idShift();
        return actionID;
    }
}
