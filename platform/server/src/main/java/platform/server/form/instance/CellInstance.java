package platform.server.form.instance;

import platform.server.form.entity.CellEntity;

public abstract class CellInstance<E extends CellEntity> {

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
