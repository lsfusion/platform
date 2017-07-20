package lsfusion.gwt.form.shared.view.filter;

import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.dto.GFilterValueDTO;

public class GPropertyFilterValue extends GFilterValue {
    public GPropertyDraw property;

    @Override
    public String toString() {
        return "Свойство";
    }

    @Override
    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(2, property.ID);
    }
}
