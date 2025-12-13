package lsfusion.client.form.object;

import lsfusion.client.form.controller.remote.serialization.ClientIdentitySerializable;

public abstract class ClientIdentityObject implements ClientIdentitySerializable {

    public int ID;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    protected String sID;

    public String getSID() {
        return sID;
    }

    public ClientIdentityObject() {
    }

    public String getLogName() {
        return getSID();
    }
}
