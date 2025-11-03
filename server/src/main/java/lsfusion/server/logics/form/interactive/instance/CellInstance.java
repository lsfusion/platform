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

    public String getSID() {
        return entity.getSID();
    }
}
