package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.ClientMessages;

import java.io.Serializable;
import java.util.Objects;

public class GDataFilterValue extends GFilterValue {
    public Serializable value;

    public GDataFilterValue() {
    }

    public GDataFilterValue(Serializable value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().filterDataValue();
    }

    @Override
    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(0, value);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GDataFilterValue && Objects.equals(value, ((GDataFilterValue) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
