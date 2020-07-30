package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.object.GObject;

import java.util.Objects;

public class GObjectFilterValue extends GFilterValue {
    public GObject object;

    @Override
    public String toString() {
        return ClientMessages.Instance.get().filterObjectValue();
    }

    @Override
    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(1, object.ID);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GObjectFilterValue && object.equals(((GObjectFilterValue) o).object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object);
    }
}
