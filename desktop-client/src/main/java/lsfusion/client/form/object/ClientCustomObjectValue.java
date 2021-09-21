package lsfusion.client.form.object;

import lsfusion.base.BaseUtils;

import java.io.Serializable;
import java.util.Objects;

public class ClientCustomObjectValue implements Serializable {

    public final long id;
    public final Long idClass;

    public ClientCustomObjectValue(long id, Long idClass) {
        this.id = id;
        this.idClass = idClass;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ClientCustomObjectValue && id == ((ClientCustomObjectValue) o).id && BaseUtils.nullEquals(idClass, ((ClientCustomObjectValue) o).idClass);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id) * 31 + BaseUtils.nullHash(idClass);
    }
}
