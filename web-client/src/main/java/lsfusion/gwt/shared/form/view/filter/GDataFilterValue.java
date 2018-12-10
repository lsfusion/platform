package lsfusion.gwt.form.shared.view.filter;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.changes.dto.GFilterValueDTO;

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
