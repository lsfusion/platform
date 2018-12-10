package lsfusion.gwt.shared.form.view.filter;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.form.view.dto.GFilterValueDTO;

import java.io.Serializable;

public class GDataFilterValue extends GFilterValue {
    public Serializable value;

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().filterDataValue();
    }

    @Override
    public GFilterValueDTO getDTO() {
        return new GFilterValueDTO(0, value);
    }
}
