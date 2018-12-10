package lsfusion.gwt.form.shared.view.filter;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GObject;
import lsfusion.gwt.form.shared.view.changes.dto.GFilterValueDTO;

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
