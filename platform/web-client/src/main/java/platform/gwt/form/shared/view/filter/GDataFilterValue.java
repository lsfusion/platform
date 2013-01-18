package platform.gwt.form.shared.view.filter;

import platform.gwt.form.shared.view.changes.dto.GFilterValueDTO;

import java.io.Serializable;

public class GDataFilterValue extends GFilterValue {
    public Serializable value;

    @Override
    public String toString() {
        return "Значение";
    }

    @Override
    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(0, value);
    }
}
