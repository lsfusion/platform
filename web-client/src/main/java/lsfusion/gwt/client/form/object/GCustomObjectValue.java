package lsfusion.gwt.client.form.object;

import lsfusion.gwt.client.base.GwtClientUtils;

import java.io.Serializable;

public class GCustomObjectValue implements Serializable {

    public long id;
    public Long idClass;

    public GCustomObjectValue() {
    }

    public GCustomObjectValue(long id, Long idClass) {
        this.id = id;
        this.idClass = idClass;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GCustomObjectValue && id == ((GCustomObjectValue) o).id && GwtClientUtils.nullEquals(idClass, ((GCustomObjectValue) o).idClass);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id) * 31 + GwtClientUtils.nullHash(idClass);
    }
}
