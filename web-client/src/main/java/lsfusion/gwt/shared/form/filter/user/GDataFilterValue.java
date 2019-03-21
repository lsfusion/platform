package lsfusion.gwt.shared.form.filter.user;

import lsfusion.gwt.client.ClientMessages;

import java.io.Serializable;

public class GDataFilterValue extends GFilterValue {
    public Serializable value;

    @Override
    public String toString() {
        return ClientMessages.Instance.get().filterDataValue();
    }

    @Override
    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(0, value);
    }
}
