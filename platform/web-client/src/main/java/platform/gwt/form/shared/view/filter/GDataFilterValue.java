package platform.gwt.form.shared.view.filter;

import java.io.Serializable;

public class GDataFilterValue extends GFilterValue {
    public Serializable value;

    @Override
    public String toString() {
        return "Значение";
    }

    @Override
    public byte getTypeID() {
        return 0;
    }
}
