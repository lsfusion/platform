package lsfusion.gwt.shared.view.filter;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.shared.view.GObject;
import lsfusion.gwt.shared.view.changes.dto.GFilterValueDTO;

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
}
