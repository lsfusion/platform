package lsfusion.gwt.shared.view.dto;

import lsfusion.gwt.shared.changes.GGroupObjectValue;

import java.io.Serializable;

public class GPropertyFilterDTO implements Serializable {
    public int propertyID;
    public GFilterValueDTO filterValue;

    public GGroupObjectValue columnKey;

    public boolean negation;
    public byte compareByte;
    public boolean junction;
}
