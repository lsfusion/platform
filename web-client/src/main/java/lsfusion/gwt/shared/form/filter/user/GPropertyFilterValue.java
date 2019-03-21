package lsfusion.gwt.shared.form.filter.user;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.shared.form.property.GPropertyDraw;

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
