package platform.gwt.form.shared.view.filter;

import platform.gwt.form.shared.view.GObject;
import platform.gwt.form.shared.view.changes.dto.GFilterValueDTO;

public class GObjectFilterValue extends GFilterValue {
    public GObject object;

    @Override
    public String toString() {
        return "Объект";
    }

    @Override
    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(1, object.ID);
    }
}
