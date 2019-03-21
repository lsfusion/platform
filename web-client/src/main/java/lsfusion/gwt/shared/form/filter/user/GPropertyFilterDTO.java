package lsfusion.gwt.shared.form.filter.user;

import lsfusion.gwt.shared.form.object.GGroupObjectValue;

import java.io.Serializable;

public class GPropertyFilterDTO implements Serializable {
    public int propertyID;
    public GFilterValueDTO filterValue;

    public GGroupObjectValue columnKey;

    public boolean negation;
    public byte compareByte;
    public boolean junction;
}
