package lsfusion.gwt.shared.form.view.filter;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.form.view.GObject;
import lsfusion.gwt.shared.form.view.dto.GFilterValueDTO;

public class GObjectFilterValue extends GFilterValue {
    public GObject object;

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().filterObjectValue();
    }

    @Override
    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(1, object.ID);
    }
}
