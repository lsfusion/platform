package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.PValue;

public class GDataFilterValue {
    public PValue value;

    public GDataFilterValue() {
    }

    public GDataFilterValue(PValue value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().filterDataValue();
    }

    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(0, PValue.remapValueBack(value));
    }

    public void setValue(PValue value) {
        this.value = value;
    }
}
