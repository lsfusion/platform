package platform.gwt.form.shared.view.changes.dto;

import java.io.Serializable;

public class GPropertyFilterDTO implements Serializable {
    public int propertyID;
    public GFilterValueDTO filterValue;

    public boolean negation;
    public byte compareByte;
    public boolean junction;
}
