package platform.client.logics;

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

    public Boolean fixedClassView = false;

    public ClientGridView gridView;
    
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
        fixedClassView = inStream.readBoolean();

        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            add(new ClientObjectImplementView(inStream,containers,this));

        gridView = new ClientGridView(inStream, containers);
    }

    private static int lastID = 0;
    private String actionID = null;
    public String getActionID() {
        if(actionID==null)
            actionID = "changeGroupObject" + (lastID++);
        return actionID;
    }
}
