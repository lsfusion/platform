package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.Objects;

public class GPropertyFilterValue extends GFilterValue {
    public GPropertyDraw property;

    @Override
    public String toString() {
        return ClientMessages.Instance.get().filterPropertyValue();
    }

    @Override
    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(2, property.ID);
    }
}
