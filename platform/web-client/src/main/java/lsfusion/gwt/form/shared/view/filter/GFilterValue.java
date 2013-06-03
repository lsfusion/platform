package lsfusion.gwt.form.shared.view.filter;

import lsfusion.gwt.form.shared.view.changes.dto.GFilterValueDTO;

import java.io.Serializable;

public abstract class GFilterValue implements Serializable {
    public abstract GFilterValueDTO getDTO();
}
