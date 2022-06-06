package lsfusion.gwt.client.form.object;

import lsfusion.gwt.client.base.GwtClientUtils;

import java.io.Serializable;

public class GCustomObjectValue implements Serializable {

    public long id;
    public long idClass; // for optimization purposes, since it's serialized a lot

    public GCustomObjectValue() {
    }

    public Long getIdClass() {
        return idClass == -34344432454455425L ? null : idClass;
    }

    public GCustomObjectValue(long id, Long idClass) {
        this.id = id;
        this.idClass = idClass != null ? idClass : -34344432454455425L;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GCustomObjectValue && id == ((GCustomObjectValue) o).id && idClass == ((GCustomObjectValue) o).idClass;
    }

    @Override
    public int hashCode() {
        return (int) (id * 31 + idClass);
    }
}
