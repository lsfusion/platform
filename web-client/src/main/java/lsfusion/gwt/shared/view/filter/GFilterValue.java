package lsfusion.gwt.shared.view.filter;

import lsfusion.gwt.shared.view.changes.dto.GFilterValueDTO;

import java.io.Serializable;

public abstract class GFilterValue implements Serializable {
    public abstract GFilterValueDTO getDTO();
}
