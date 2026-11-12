package lsfusion.server.logics.form.interactive.instance;

import lsfusion.base.identity.IdentityObject;

public abstract class CellInstance<E extends IdentityObject> {

    public E entity;

    public CellInstance(E entity) {
        this.entity = entity;
    }

    public int getID() {
        return entity.getID();
    }

    // backward compatibility
    @Deprecated //deprecated since 2.0, removed in 7.0
    public String getsID() {
        return getSID();
    }
    public String getSID() {
        return entity.getSID();
    }
}
