package platform.server.form.instance;

import platform.base.identity.IdentityObject;

public abstract class CellInstance<E extends IdentityObject> {

    protected E entity;

    public CellInstance(E entity) {
        this.entity = entity;
    }

    public int getID() {
        return entity.getID();
    }

    public String getsID() {
        return entity.getSID();
    }
}
