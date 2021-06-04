package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.ClientMessages;

import java.io.Serializable;

public class GDataFilterValue {
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

    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(0, value);
    }
}
