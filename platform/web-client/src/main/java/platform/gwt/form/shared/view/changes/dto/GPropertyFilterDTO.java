package platform.gwt.form.shared.view.changes.dto;

import platform.gwt.form.shared.view.filter.GFilterValue;

import java.io.Serializable;

public class GPropertyFilterDTO implements Serializable {
    public int propertyID;
    public GFilterValue filterValue;

    public boolean negation;
    public byte compareByte;
    public boolean junction;
}
