package platform.client.logics;

import platform.base.DefaultIDGenerator;
import platform.base.IDGenerator;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class ClientGroupObject extends ArrayList<ClientObject>
                                 implements Serializable {

    private Integer ID = 0;

    public Integer getID() {
        return ID;
    }

    public byte banClassView = 0;

    public ClientGrid grid;
    public ClientShowType showType;

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

    public ClientGroupObject(DataInputStream inStream, Collection<ClientContainer> containers) throws IOException, ClassNotFoundException {
        ID = inStream.readInt();
        banClassView = inStream.readByte();

        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            add(new ClientObject(inStream,containers,this));

        grid = new ClientGrid(inStream, containers);
        showType = new ClientShowType(inStream, containers);
    }

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private String actionID = null;
    public String getActionID() {
        if(actionID==null)
            actionID = "changeGroupObject" + idGenerator.idShift();
        return actionID;
    }
}
