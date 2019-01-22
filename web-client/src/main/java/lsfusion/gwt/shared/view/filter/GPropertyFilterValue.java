package lsfusion.gwt.shared.view.filter;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.changes.dto.GFilterValueDTO;

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
