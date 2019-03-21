package lsfusion.gwt.shared.form.filter.user;

import lsfusion.gwt.shared.form.filter.user.GFilterValueDTO;

import java.io.Serializable;

public abstract class GFilterValue implements Serializable {
    public abstract GFilterValueDTO getDTO();
}
