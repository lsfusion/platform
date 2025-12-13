package lsfusion.server.logics.form.struct;

import lsfusion.base.identity.IDGenerator;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.ServerIdentityObject;

public abstract class IdentityEntity<This extends IdentityEntity<This, AddParent>, AddParent extends IdentityEntity<AddParent, ?>> extends ServerIdentityObject<This, AddParent> {

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

    @Override
    public String toString() {
        return getSID();
    }

    public void setSID(String sID) {
        this.sID = sID;
    }

    public IdentityEntity(IDGenerator ID, String sID, String defaultName) {
        this.ID = ID.id();
        this.sID = sID != null ? sID : defaultName + this.ID;
    }

    protected IdentityEntity(This src, ObjectMapping mapping) {
        super(src, mapping);

        this.ID = mapping.id();
        this.sID = src.sID;
    }

    @Override
    public AddParent getAddParent(ObjectMapping mapping) {
        return null;
    }
    @Override
    public This getAddChild(AddParent addParent, ObjectMapping mapping) {
        return null;
    }
}
