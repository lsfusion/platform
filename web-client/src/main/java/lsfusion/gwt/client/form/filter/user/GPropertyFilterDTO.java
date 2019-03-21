package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.io.Serializable;

public class GPropertyFilterDTO implements Serializable {
    public int propertyID;
    public GFilterValueDTO filterValue;

    public GGroupObjectValue columnKey;

    public boolean negation;
    public byte compareByte;
    public boolean junction;
}
