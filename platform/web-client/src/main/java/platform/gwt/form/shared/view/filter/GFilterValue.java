package platform.gwt.form.shared.view.filter;

import platform.gwt.form.shared.view.changes.dto.GFilterValueDTO;

import java.io.Serializable;

public abstract class GFilterValue implements Serializable {
    public abstract GFilterValueDTO getDTO();
}
